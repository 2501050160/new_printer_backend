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

    @Autowired
    private com.saipraveen.login_registration.repository.CampusBlockRepository campusBlockRepository;

    public boolean isOtpRequiredForOrder(PdfFile pdf) {
        if (pdf == null) return true;

        String channel = (pdf.getOrderChannel() != null && !pdf.getOrderChannel().isEmpty())
                ? pdf.getOrderChannel().toUpperCase().trim()
                : "WEB";
        String block = pdf.getBlockLocation() != null ? pdf.getBlockLocation().trim() : "";
        String college = "KLU";
        if (!block.isEmpty() && campusBlockRepository != null) {
            try {
                com.saipraveen.login_registration.entity.CampusBlock blk = campusBlockRepository.findByName(block);
                if (blk != null && blk.getCollege() != null && !blk.getCollege().isEmpty()) {
                    college = blk.getCollege().trim();
                }
            } catch (Exception ignored) {}
        }

        // 1. Block-Level Channel Override e.g. "otp_required_whatsapp_C Block" or "otp_required_web_C Block"
        if (!block.isEmpty()) {
            String blockChannelKey = "otp_required_" + channel.toLowerCase() + "_" + block;
            String blockChannelVal = systemSettingService.getSetting(blockChannelKey, null);
            if (blockChannelVal != null && !blockChannelVal.isEmpty()) {
                return Boolean.parseBoolean(blockChannelVal);
            }

            // Generic Block Override e.g. "otp_required_block_C Block"
            String blockKey = "otp_required_block_" + block;
            String blockVal = systemSettingService.getSetting(blockKey, null);
            if (blockVal != null && !blockVal.isEmpty()) {
                return Boolean.parseBoolean(blockVal);
            }

            // PrinterConfig hardware fallback
            try {
                com.saipraveen.login_registration.entity.PrinterConfig config = printerConfigService.getPrinterByBlock(block);
                if (config != null && config.getOtpEnabled() != null) {
                    if (Boolean.FALSE.equals(config.getOtpEnabled())) {
                        return false;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. College-Level Channel Override e.g. "otp_required_whatsapp_KLU" or "otp_required_web_KLU"
        if (!college.isEmpty()) {
            String collegeChannelKey = "otp_required_" + channel.toLowerCase() + "_" + college;
            String collegeChannelVal = systemSettingService.getSetting(collegeChannelKey, null);
            if (collegeChannelVal != null && !collegeChannelVal.isEmpty()) {
                return Boolean.parseBoolean(collegeChannelVal);
            }

            String collegeKey = "otp_required_college_" + college;
            String collegeVal = systemSettingService.getSetting(collegeKey, null);
            if (collegeVal != null && !collegeVal.isEmpty()) {
                return Boolean.parseBoolean(collegeVal);
            }
        }

        // 3. Global Channel Settings (defaults to true)
        if ("WHATSAPP".equalsIgnoreCase(channel)) {
            return systemSettingService.getSettingBool("whatsapp_otp_required", true);
        } else {
            return systemSettingService.getSettingBool("web_otp_required", true);
        }
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void promoteExpiredCancelWindows() {

        List<PdfFile> expired =
                repository.findExpiredCancelWindows(
                        LocalDateTime.now()
                );

        for (PdfFile pdf : expired) {
            if (!isOtpRequiredForOrder(pdf)) {
                repository.updateStatusAndQueuedAtByOrderId(pdf.getOrderId(), "QUEUE", LocalDateTime.now());
                System.out.println("Order promoted directly to QUEUE (OTP bypassed): " + pdf.getOrderId());
            } else {
                repository.updateStatusByOrderId(pdf.getOrderId(), "PENDING_SCAN");
                System.out.println("Order held in PENDING_SCAN for OTP verification: " + pdf.getOrderId());
            }
        }
    }

    @Scheduled(fixedRate = 15000)
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

        // 5-Minute stuck PRINTING orders timeout (auto-refund and remove from display screen if agent crashed)
        LocalDateTime printingCutoff = LocalDateTime.now().minusMinutes(5);
        List<PdfFile> stuckPrinting = repository.findStuckPrintingOrders(printingCutoff);
        for (PdfFile pdf : stuckPrinting) {
            refundAndCancel(
                    pdf,
                    "Printing failed or timed out (exceeded 5 minutes in PRINTING state)"
            );

            System.out.println(
                    "Stuck PRINTING order timed out (5 mins), refunded and cancelled: "
                            + pdf.getOrderId()
            );
        }

        // 10-Minute PENDING_SCAN verification timeout (auto-refund if no OTP entered)
        LocalDateTime scanCutoff = LocalDateTime.now().minusMinutes(10);
        List<PdfFile> scanTimedOut = repository.findExpiredPendingScanOrders(scanCutoff);
        for (PdfFile pdf : scanTimedOut) {
            refundAndCancel(
                    pdf,
                    "QR/OTP Scan verification timeout (10 minutes)"
            );

            System.out.println(
                    "Order scan verification timed out (10 mins), refunded and deleted file data: "
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
            String newStatus = "PENDING_SCAN";
            LocalDateTime queuedAt = null;
            if (!isOtpRequiredForOrder(pdf)) {
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

        boolean isPaid = "PAID".equalsIgnoreCase(pdf.getPaymentStatus());
        Double refundAmount = (isPaid && pdf.getPrice() != null) ? pdf.getPrice() : 0.0;

        if (isPaid && pdf.getUserId() != null && refundAmount > 0) {
            userService.creditWallet(
                    pdf.getUserId(),
                    refundAmount
            );
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus(isPaid ? "REFUNDED" : "CANCELLED_UNPAID");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        repository.save(pdf);

        result.put("success", true);
        result.put("message", isPaid ? "Order cancelled. Amount credited to wallet." : "Unpaid order cancelled. No charges applied.");
        result.put("refundAmount", refundAmount);

        return result;
    }

    @Transactional
    public Map<String, Object> flushOrder(String orderId) {
        Map<String, Object> result = new HashMap<>();
        if (orderId == null || orderId.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Order ID cannot be empty");
            return result;
        }

        PdfFile pdf = repository.findByOrderId(orderId.trim());
        if (pdf == null) {
            try {
                String digits = orderId.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    long id = Long.parseLong(digits);
                    pdf = repository.findById(id).orElse(null);
                }
            } catch (Exception ignored) {}
        }

        if (pdf == null) {
            result.put("success", false);
            result.put("message", "Order not found: " + orderId);
            return result;
        }

        Double refundAmount = (pdf.getPrice() != null) ? pdf.getPrice() : 0.0;
        if (pdf.getUserId() != null && refundAmount > 0 && !"REFUNDED".equals(pdf.getPaymentStatus())) {
            try {
                userService.creditWallet(pdf.getUserId(), refundAmount);
            } catch (Exception e) {
                System.err.println("Failed to refund during flush: " + e.getMessage());
            }
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);
        repository.save(pdf);

        if (sseService != null) {
            sseService.broadcastOrderEvent(pdf.getOrderId(), "CANCELLED");
            sseService.broadcastQueueEvent("Order flushed and removed from queue: " + pdf.getOrderId());
        }

        result.put("success", true);
        result.put("message", "Order " + orderId + " successfully flushed, cancelled, and refunded.");
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
            if (!isOtpRequiredForOrder(pdf)) {
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
        boolean isPaid = "PAID".equalsIgnoreCase(pdf.getPaymentStatus());
        Double refundAmount = (isPaid && pdf.getPrice() != null) ? pdf.getPrice() : 0.0;

        if (isPaid && pdf.getUserId() != null && refundAmount > 0) {
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
        pdf.setPaymentStatus(isPaid ? "REFUNDED" : "CANCELLED_UNPAID");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        PdfFile saved = repository.save(pdf);

        try {
            if (sseService != null) {
                sseService.broadcastOrderEvent(saved.getOrderId(), "CANCELLED");
                sseService.broadcastQueueEvent("Order expired/cancelled: " + saved.getOrderId());
            }
        } catch (Exception e) {
            System.err.println("SSE broadcast error on refund/cancel for " + saved.getOrderId() + ": " + e.getMessage());
        }
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
            if (!isOtpRequiredForOrder(pdf)) {
                repository.updateStatusAndQueuedAtByOrderId(pdf.getOrderId(), "QUEUE", LocalDateTime.now());
                System.out.println("Scheduled order promoted directly to QUEUE (OTP bypassed): " + pdf.getOrderId());
            } else {
                repository.updateStatusAndCancelWindowEndsAtByOrderId(pdf.getOrderId(), "PENDING_SCAN", LocalDateTime.now().plusSeconds(30));
                System.out.println("Scheduled order held in PENDING_SCAN for OTP: " + pdf.getOrderId());
            }
        }
    }
}
