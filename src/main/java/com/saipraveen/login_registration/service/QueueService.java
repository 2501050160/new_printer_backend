package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.repository.PdfFileRepository;

@Service
public class QueueService {

    @Autowired
    private PdfFileRepository repository;

    @Autowired
    private UserService userService;

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private com.saipraveen.login_registration.service.SystemSettingService systemSettingService;

    @Autowired
    private SseService sseService;

    @Value("${print.cancel-window-seconds:30}")
    private int cancelWindowSeconds;

    @Value("${print.fulfillment-timeout-minutes:30}")
    private int fulfillmentTimeoutMinutes;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void promoteExpiredCancelWindows() {

        List<PdfFile> expired =
                repository.findExpiredCancelWindows(
                        LocalDateTime.now()
                );

        for (PdfFile pdf : expired) {
            com.saipraveen.login_registration.entity.PrinterConfig config = null;
            try {
                config = printerConfigService.getPrinterByBlock(pdf.getBlockLocation());
            } catch (Exception e) {
                System.err.println("Failed to fetch printer config: " + e.getMessage());
            }

            if (config != null && Boolean.FALSE.equals(config.getOtpEnabled())) {
                repository.updateStatusAndQueuedAtByOrderId(pdf.getOrderId(), "QUEUE", LocalDateTime.now());
                System.out.println("Order promoted directly to QUEUE (OTP disabled for block): " + pdf.getOrderId());
            } else {
                repository.updateStatusByOrderId(pdf.getOrderId(), "PENDING_SCAN");
                System.out.println("Order held in PENDING_SCAN for OTP verification: " + pdf.getOrderId());
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cancelTimedOutOrders() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(
                                fulfillmentTimeoutMinutes
                        );

        List<PdfFile> timedOut =
                repository.findTimedOutOrders(cutoff);

        for (PdfFile pdf : timedOut) {

            refundAndCancel(
                    pdf,
                    "Print not completed within "
                            + fulfillmentTimeoutMinutes
                            + " minutes"
            );

            System.out.println(
                    "Order timed out and refunded: "
                            + pdf.getOrderId()
                            + " (Print not completed within "
                            + fulfillmentTimeoutMinutes
                            + " minutes)"
            );
        }

        // 15-Minute PENDING_SCAN verification timeout (auto-refund if no OTP entered)
        LocalDateTime scanCutoff = LocalDateTime.now().minusMinutes(15);
        List<PdfFile> scanTimedOut = repository.findExpiredPendingScanOrders(scanCutoff);
        for (PdfFile pdf : scanTimedOut) {
            refundAndCancel(
                    pdf,
                    "QR/OTP Scan verification timeout (15 minutes)"
            );

            System.out.println(
                    "Order scan verification timed out, refunded and deleted file data: "
                            + pdf.getOrderId()
            );
        }
    }

    public List<PdfFile> getQueueByBlock(String blockLocation) {

        return repository.findQueueByBlock(
                normalizeBlock(blockLocation)
        );
    }

    public List<PdfFile> getActiveQueueByBlock(String blockLocation) {

        return repository.findActiveQueueByBlock(
                normalizeBlock(blockLocation)
        );
    }

    public PdfFile getNextForAgent(String blockLocation) {
        recordHeartbeat(blockLocation);

        List<PdfFile> queue =
                repository.findQueueByBlock(
                        normalizeBlock(blockLocation)
                );

        if (queue.isEmpty() && blockLocation != null) {
            queue = repository.findQueueByBlock(blockLocation.trim());
        }

        if (queue.isEmpty()) {
            // Check all queued orders and match fuzzily by block name
            List<PdfFile> allQueued = repository.findAllQueuedOrders();
            if (blockLocation != null) {
                String cleanTarget = blockLocation.toLowerCase().replaceAll("[\\s-_]", "");
                for (PdfFile p : allQueued) {
                    if (p.getBlockLocation() != null) {
                        String cleanOrderBlock = p.getBlockLocation().toLowerCase().replaceAll("[\\s-_]", "");
                        if (cleanOrderBlock.equals(cleanTarget) || cleanOrderBlock.contains(cleanTarget) || cleanTarget.contains(cleanOrderBlock)) {
                            return p;
                        }
                    }
                }
            }
            if (!allQueued.isEmpty()) {
                for (PdfFile p : allQueued) {
                    if (p.getBlockLocation() == null || p.getBlockLocation().trim().isEmpty() || "Campus Kiosk".equalsIgnoreCase(p.getBlockLocation())) {
                        return p;
                    }
                }
            }
            return null;
        }

        return queue.get(0);
    }

    public PdfFile startPrinting(String orderId) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }

        if (!"QUEUE".equals(pdf.getStatus()) && !"PRINTING".equals(pdf.getStatus())) {
            throw new RuntimeException(
                    "Order is not ready for printing: " + pdf.getStatus()
            );
        }

        pdf.setStatus("PRINTING");
        pdf.setPrintingStartedAt(LocalDateTime.now());

        PdfFile saved = repository.save(pdf);
        sseService.broadcastOrderEvent(orderId, "PRINTING");
        sseService.broadcastQueueEvent("Order " + orderId + " is PRINTING");
        return saved;
    }

    @Transactional
    public PdfFile proceedOrder(String orderId) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }
        if ("CANCEL_WINDOW".equals(pdf.getStatus())) {
            com.saipraveen.login_registration.entity.PrinterConfig config = null;
            try {
                config = printerConfigService.getPrinterByBlock(pdf.getBlockLocation());
            } catch (Exception e) {
                System.err.println("Failed to fetch printer config: " + e.getMessage());
            }

            String newStatus = "PENDING_SCAN";
            LocalDateTime queuedAt = null;
            if (config != null && Boolean.FALSE.equals(config.getOtpEnabled())) {
                newStatus = "QUEUE";
                queuedAt = LocalDateTime.now();
                repository.updateStatusAndQueuedAtByOrderId(orderId, newStatus, queuedAt);
                pdf.setQueuedAt(queuedAt);
            } else {
                repository.updateStatusByOrderId(orderId, newStatus);
            }
            pdf.setStatus(newStatus);
            sseService.broadcastOrderEvent(orderId, newStatus);
            sseService.broadcastQueueEvent("Order " + orderId + " moved to " + newStatus);
        }
        return pdf;
    }

    public PdfFile completeOrder(String orderId) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }

        pdf.setStatus("COMPLETED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null); // Delete the PDF binary file data immediately after printing is completed

        try {
            int totalDocPages = (pdf.getTotalPages() != null ? pdf.getTotalPages() : 1);
            int sheetsPerCopy = Boolean.TRUE.equals(pdf.getDoubleSided()) ? (int) Math.ceil(totalDocPages / 2.0) : totalDocPages;
            int totalSheets = sheetsPerCopy * (pdf.getCopies() != null ? pdf.getCopies() : 1);
            printerConfigService.decrementPaper(pdf.getBlockLocation(), totalSheets);
        } catch (Exception e) {
            System.err.println("Failed to decrement paper count for block: " + pdf.getBlockLocation() + " - " + e.getMessage());
        }

        PdfFile saved = repository.save(pdf);
        sseService.broadcastOrderEvent(orderId, "COMPLETED");
        sseService.broadcastQueueEvent("Order " + orderId + " COMPLETED");
        return saved;
    }

    @Transactional
    public Map<String, Object> cancelOrder(
            String orderId,
            Long userId
    ) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        Map<String, Object> result =
                new HashMap<>();

        if (pdf == null) {
            result.put("success", false);
            result.put("message", "Order not found");
            return result;
        }

        boolean isCancelable = "CANCEL_WINDOW".equals(pdf.getStatus()) || "PENDING_SCAN".equals(pdf.getStatus());

        if (!isCancelable) {
            result.put("success", false);
            result.put("message", "Order cannot be cancelled at this stage");
            return result;
        }

        if ("CANCEL_WINDOW".equals(pdf.getStatus())) {
            if (pdf.getCancelWindowEndsAt() != null
                    && LocalDateTime.now().isAfter(
                            pdf.getCancelWindowEndsAt()
                    )) {
                result.put("success", false);
                result.put("message", "Cancel window has expired");
                return result;
            }
        }

        if (userId != null
                && pdf.getUserId() != null
                && !userId.equals(pdf.getUserId())) {

            result.put("success", false);
            result.put("message", "Unauthorized");
            return result;
        }

        Double refundAmount =
                pdf.getPrice() == null
                        ? 0.0
                        : pdf.getPrice();

        if (pdf.getUserId() != null && refundAmount > 0) {
            userService.creditWallet(
                    pdf.getUserId(),
                    refundAmount
            );
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        repository.save(pdf);

        result.put("success", true);
        result.put("message", "Order cancelled. Amount credited to wallet.");
        result.put("refundAmount", refundAmount);

        return result;
    }

    public void beginCancelWindow(PdfFile pdf) {

        LocalDateTime now = LocalDateTime.now();

        pdf.setPaidAt(now);
        pdf.setPaymentStatus("PAID");

        // Generate random 4-digit OTP if not already set
        if (pdf.getOtpCode() == null || pdf.getOtpCode().trim().isEmpty()) {
            int randomOtp = 1000 + new java.util.Random().nextInt(9000);
            pdf.setOtpCode(String.valueOf(randomOtp));
        }

        if (pdf.getScheduledTime() != null) {
            pdf.setStatus("SCHEDULED");
        } else {
            pdf.setCancelWindowEndsAt(now);
            com.saipraveen.login_registration.entity.PrinterConfig config = null;
            try {
                config = printerConfigService.getPrinterByBlock(pdf.getBlockLocation());
            } catch (Exception e) {
                System.err.println("Failed to fetch printer config: " + e.getMessage());
            }
            if (config != null && Boolean.FALSE.equals(config.getOtpEnabled())) {
                pdf.setStatus("QUEUE");
                pdf.setQueuedAt(now);
            } else {
                pdf.setStatus("PENDING_SCAN");
            }
        }

        if (pdf.getOriginalPrice() == null && pdf.getPrice() != null) {
            pdf.setOriginalPrice(pdf.getPrice());
        }
    }

    private void refundAndCancel(PdfFile pdf, String reason) {

        Double refundAmount =
                pdf.getPrice() == null
                        ? 0.0
                        : pdf.getPrice();

        if (pdf.getUserId() != null && refundAmount > 0) {
            try {
                if (userService.userExists(pdf.getUserId())) {
                    userService.creditWallet(
                            pdf.getUserId(),
                            refundAmount
                    );
                } else {
                    System.err.println("Could not refund order " + pdf.getOrderId() + " because user " + pdf.getUserId() + " does not exist.");
                }
            } catch (Exception e) {
                System.err.println("Could not refund order " + pdf.getOrderId() + " to user " + pdf.getUserId() + ": " + e.getMessage());
            }
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        repository.save(pdf);
    }

    public int getCancelWindowSeconds() {
        return cancelWindowSeconds;
    }

    private String normalizeBlock(String blockLocation) {

        if (blockLocation == null
                || blockLocation.trim().isEmpty()) {

            return "C Block";
        }

        return blockLocation.trim();
    }

    private static final java.util.Map<String, LocalDateTime> agentHeartbeats = new java.util.concurrent.ConcurrentHashMap<>();

    public void recordHeartbeat(String blockLocation) {
        if (blockLocation != null) {
            String normalized = normalizeBlock(blockLocation);
            agentHeartbeats.put(normalized, LocalDateTime.now());
        }
    }

    public boolean isAgentOnline(String blockLocation) {
        if (blockLocation == null) {
            return false;
        }
        try {
            com.saipraveen.login_registration.entity.PrinterConfig config = printerConfigService.getPrinterByBlock(blockLocation);
            if (config != null && Boolean.TRUE.equals(config.getActive()) && !Boolean.TRUE.equals(config.getPaused()) && !Boolean.TRUE.equals(config.getMaintenance())) {
                return true;
            }
        } catch (Exception e) {
            // fallback to recorded heartbeat if config lookup fails
        }
        String normalized = normalizeBlock(blockLocation);
        LocalDateTime lastHeartbeat = agentHeartbeats.get(normalized);
        if (lastHeartbeat == null) {
            return false;
        }
        return lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(10));
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    @Transactional
    public void promoteScheduledOrders() {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(5);
        List<PdfFile> pendingScheduled = repository.findPendingScheduledOrders(cutoff);
        for (PdfFile pdf : pendingScheduled) {
            com.saipraveen.login_registration.entity.PrinterConfig config = null;
            try {
                config = printerConfigService.getPrinterByBlock(pdf.getBlockLocation());
            } catch (Exception e) {
                System.err.println("Failed to fetch printer config: " + e.getMessage());
            }

            if (config != null && Boolean.FALSE.equals(config.getOtpEnabled())) {
                repository.updateStatusAndQueuedAtByOrderId(pdf.getOrderId(), "QUEUE", LocalDateTime.now());
                System.out.println("Scheduled order promoted directly to QUEUE (OTP disabled): " + pdf.getOrderId());
            } else {
                repository.updateStatusAndCancelWindowEndsAtByOrderId(pdf.getOrderId(), "PENDING_SCAN", LocalDateTime.now().plusSeconds(30));
                System.out.println("Scheduled order held in PENDING_SCAN for OTP: " + pdf.getOrderId());
            }
        }
    }
}
