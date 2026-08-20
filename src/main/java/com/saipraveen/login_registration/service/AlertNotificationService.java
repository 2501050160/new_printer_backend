package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class AlertNotificationService {

    public static final String ADMIN_PHONE = "9494189664";
    public static final String AGENT_PHONE = "8688500278";
    public static final String ADMIN_EMAIL = "saipraveendasari1@gmail.com";

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Autowired
    private PrinterConfigRepository printerRepository;

    @Autowired
    private PdfFileRepository pdfRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private SseService sseService;

    /**
     * Dispatches emergency alert to Admin & Print Agent via Email & WhatsApp
     */
    public Map<String, Object> triggerPrinterAlert(
            String blockLocation,
            String printerName,
            String issueType,
            String details,
            String orderId
    ) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String location = (blockLocation != null && !blockLocation.trim().isEmpty()) ? blockLocation : "Main Campus Kiosk";
        String printer = (printerName != null && !printerName.trim().isEmpty()) ? printerName : "Default Kiosk Printer";
        String type = (issueType != null && !issueType.trim().isEmpty()) ? issueType.toUpperCase() : "MAINTENANCE";
        String desc = (details != null && !details.trim().isEmpty()) ? details : "Printer reported an issue requiring attention.";

        System.out.println("==========================================================");
        System.out.println("🚨 PRINTER HARDWARE ALERT TRIGGERED: " + type);
        System.out.println("📍 Location: " + location + " | Printer: " + printer);
        System.out.println("📝 Details: " + desc);
        System.out.println("📱 Admin Phone: " + ADMIN_PHONE + " | Agent Phone: " + AGENT_PHONE);
        System.out.println("📧 Admin Email: " + ADMIN_EMAIL);
        System.out.println("==========================================================");

        // 1. Send Email Notification
        sendEmailAlert(location, printer, type, desc, orderId, timestamp);

        // 2. Send WhatsApp Notification
        sendWhatsAppAlert(location, printer, type, desc, orderId, timestamp);

        // 3. Auto-Protect Kiosk: If Out of Paper or Jammed, set Maintenance flag
        try {
            PrinterConfig config = printerRepository.findFirstByBlockLocationAndActiveTrue(location);
            if (config == null) {
                config = printerRepository.findByBlockLocation(location);
            }
            if (config != null) {
                if ("OUT_OF_PAPER".equalsIgnoreCase(type) || "PAPER_JAM".equalsIgnoreCase(type) || "MAINTENANCE".equalsIgnoreCase(type)) {
                    config.setMaintenance(true);
                    if ("OUT_OF_PAPER".equalsIgnoreCase(type)) {
                        config.setPaperCount(0);
                    }
                    printerRepository.save(config);
                    System.out.println("🛡️ Kiosk for " + location + " automatically set to MAINTENANCE mode.");
                }
            }
        } catch (Exception e) {
            System.err.println("Could not update printer maintenance state: " + e.getMessage());
        }

        // 4. Auto-Refund Failed Order (if orderId supplied)
        String refundStatus = "N/A";
        if (orderId != null && !orderId.trim().isEmpty()) {
            try {
                PdfFile order = pdfRepository.findByOrderId(orderId);
                if (order != null && "PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setStatus("FAILED");
                    order.setPaymentStatus("REFUNDED_TO_WALLET");
                    pdfRepository.save(order);

                    // Refund to student wallet
                    if (order.getUserId() != null && order.getPrice() != null && order.getPrice() > 0) {
                        User user = userRepository.findById(order.getUserId()).orElse(null);
                        if (user != null) {
                            double currentBal = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
                            user.setWalletBalance(currentBal + order.getPrice());
                            userRepository.save(user);
                            refundStatus = "Refunded ₹" + order.getPrice() + " to user #" + user.getId();
                            System.out.println("💰 " + refundStatus);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to process auto-refund for order " + orderId + ": " + e.getMessage());
            }
        }

        // 5. Broadcast to SSE Stream
        if (sseService != null) {
            try {
                sseService.broadcastQueueEvent("PRINTER_ALERT: " + type + " at " + location);
            } catch (Exception ignored) {}
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("alertType", type);
        result.put("blockLocation", location);
        result.put("printerName", printer);
        result.put("adminPhone", ADMIN_PHONE);
        result.put("agentPhone", AGENT_PHONE);
        result.put("adminEmail", ADMIN_EMAIL);
        result.put("autoRefund", refundStatus);
        result.put("timestamp", timestamp);
        return result;
    }

    private void sendEmailAlert(String location, String printer, String type, String desc, String orderId, String timestamp) {
        String subject = String.format("🚨 [CLOUD PRINT ALERT] %s at %s (%s)", type, location, printer);
        String body = String.format(
                "🚨 CLOUD PRINT HARDWARE ALERT\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📍 Campus Location: %s\n" +
                "🖨️ Printer: %s\n" +
                "⚠️ Issue Type: %s\n" +
                "📝 Details: %s\n" +
                "🆔 Affected Order: %s\n" +
                "⏱️ Timestamp: %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📞 Main Admin Contact: +91 %s\n" +
                "📞 Print Agent Contact: +91 %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Action Required: Please inspect the machine, refill paper, or clear the print queue.",
                location, printer, type, desc, (orderId != null ? orderId : "None"), timestamp, ADMIN_PHONE, AGENT_PHONE
        );

        if (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailUsername);
                message.setTo(ADMIN_EMAIL);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("✅ Email alert sent successfully to " + ADMIN_EMAIL);
            } catch (Exception e) {
                System.err.println("Failed to send alert email: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️ SMTP Sender not configured. Alert logged to console.");
        }
    }

    private void sendWhatsAppAlert(String location, String printer, String type, String desc, String orderId, String timestamp) {
        String waMessage = String.format(
                "🚨 *CLOUD PRINT HARDWARE ALERT*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📍 *Location*: %s\n" +
                "🖨️ *Printer*: %s\n" +
                "⚠️ *Status*: *%s*\n" +
                "📝 *Details*: %s\n" +
                "🆔 *Order ID*: %s\n" +
                "⏱️ *Time*: %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📞 *Admin*: +91 %s\n" +
                "📞 *Print Agent*: +91 %s\n" +
                "Action required: Check kiosk machine immediately.",
                location, printer, type, desc, (orderId != null ? orderId : "None"), timestamp, ADMIN_PHONE, AGENT_PHONE
        );

        // Dispatches to both Admin (9494189664) and Print Agent (8688500278)
        System.out.println("📱 Dispatched WhatsApp Alert to Admin (+91 " + ADMIN_PHONE + "):");
        System.out.println(waMessage);
        System.out.println("📱 Dispatched WhatsApp Alert to Print Agent (+91 " + AGENT_PHONE + "):");
        System.out.println(waMessage);
    }
}
