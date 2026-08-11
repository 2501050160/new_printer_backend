package com.saipraveen.login_registration.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.service.PdfFileService;

@RestController
@RequestMapping("/api/bot")
@CrossOrigin(originPatterns = "*")
public class BotPrintController {

    @Autowired
    private PdfFileService pdfFileService;

    @Autowired
    private com.saipraveen.login_registration.service.PrinterConfigService printerConfigService;

    @Autowired
    private com.saipraveen.login_registration.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:https://cloudprint.website}")
    private String frontendUrl;

    private String sanitizePhoneNumber(String raw) {
        if (raw == null) return "0000000000";
        String clean = raw.replaceAll("[^0-9]", "");
        if (clean.startsWith("91") && clean.length() == 12) {
            clean = clean.substring(2);
        }
        return clean.isEmpty() ? "0000000000" : clean;
    }

    @PostMapping(value = "/direct-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> directUploadAndOrder(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "blockLocation", required = false) String blockLocation,
            @RequestParam(value = "printType", defaultValue = "BW") String printType,
            @RequestParam(value = "selectedPages", defaultValue = "ALL") String selectedPages,
            @RequestParam(value = "doubleSided", defaultValue = "false") Boolean doubleSided,
            @RequestParam(value = "copies", defaultValue = "1") Integer copies
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file provided");
            }

            // Check if the printer for the requested block is online and operational
            com.saipraveen.login_registration.service.PrinterConfigService.AvailabilityResult avail = 
                printerConfigService.checkPrinterAvailability(blockLocation, printType);
            if (!avail.isAvailable()) {
                return ResponseEntity.badRequest().body("⚠️ Kiosk Offline: " + avail.getMessage());
            }

            // Ensure each unique WhatsApp phone number gets its OWN User account in DB
            String cleanPhone = sanitizePhoneNumber(phoneNumber);
            String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";

            String displayName = customerName != null && !customerName.isEmpty()
                ? customerName.replaceAll("\\s*\\(\\d+\\)", "").trim()
                : "Student";
            String fullNameWithPhone = displayName + " (+91 " + cleanPhone + ")";

            com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
            if (user == null) {
                user = new com.saipraveen.login_registration.entity.User();
                user.setName(fullNameWithPhone);
                user.setEmail(waEmail);
                user.setPassword("WA_BOT_USER_NOPASS");
                user.setWalletBalance(0.0);
                user.setReferralCode("WA_" + cleanPhone);
                user = userRepository.save(user);
            } else if (!user.getName().contains("+91 " + cleanPhone)) {
                user.setName(fullNameWithPhone);
                user = userRepository.save(user);
            }

            // Save PDF to DB linked to the unique WhatsApp User ID
            PdfFile pdf = pdfFileService.savePdf(file, user.getId(), fullNameWithPhone, blockLocation);

            // Update order details to generate real Order ID
            PdfFile updated = pdfFileService.updateOrder(
                    pdf.getOrderId(),
                    copies != null ? copies : 1,
                    selectedPages != null ? selectedPages : "ALL",
                    printType,
                    blockLocation,
                    "1-up",
                    doubleSided != null ? doubleSided : false
            );

            // Calculate details from updated PDF
            int pages = updated.getTotalPages() != null ? updated.getTotalPages() : 1;
            double rate = "COLOR".equalsIgnoreCase(printType) ? 5.0 : 2.0;
            double estimatedTotal = updated.getPrice() != null ? updated.getPrice() : (pages * rate);

            // Generate 4-digit OTP from order ID and attach to order
            String otp = String.format("%04d", (updated.getId() != null ? updated.getId() : 1000) % 10000);
            pdfFileService.updateStatusAndOtp(updated.getId(), "ORDER_CREATED", otp);

            String realOrderId = updated.getOrderId() != null ? updated.getOrderId() : pdf.getOrderId();
            String checkoutUrl = frontendUrl + "/pay?orderId=" + realOrderId;

            StringBuilder botMsg = new StringBuilder();
            botMsg.append("🖨️ *Cloud Print Order Created!*\n");
            botMsg.append("-----------------------------\n");
            botMsg.append("📄 *File*: ").append(file.getOriginalFilename()).append("\n");
            botMsg.append("📊 *Pages*: ").append(pages).append(" | *Print Type*: ").append(printType).append("\n");
            botMsg.append("💰 *Total Amount*: ₹").append(String.format("%.2f", estimatedTotal)).append("\n");
            botMsg.append("🔐 *Your 4-Digit OTP*: *").append(otp).append("*\n");
            botMsg.append("📍 *Target Kiosk*: ").append(blockLocation).append("\n\n");
            botMsg.append("👉 *Complete Payment*: ").append(checkoutUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", realOrderId);
            response.put("otp", otp);
            response.put("totalPages", pages);
            response.put("estimatedTotal", estimatedTotal);
            response.put("blockLocation", blockLocation);
            response.put("paymentUrl", checkoutUrl);
            response.put("botMessage", botMsg.toString());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to process uploaded file: " + e.getMessage());
        }
    }

    @Autowired
    private com.saipraveen.login_registration.repository.CouponRepository couponRepository;

    @org.springframework.web.bind.annotation.GetMapping("/user-balance")
    public ResponseEntity<?> getUserBalance(@RequestParam String phoneNumber) {
        String cleanPhone = sanitizePhoneNumber(phoneNumber);
        String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";
        com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
        double balance = (user != null && user.getWalletBalance() != null) ? user.getWalletBalance() : 0.0;
        Map<String, Object> res = new HashMap<>();
        res.put("phoneNumber", cleanPhone);
        res.put("balance", balance);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/redeem-coupon")
    public ResponseEntity<?> redeemCoupon(
            @RequestParam String phoneNumber,
            @RequestParam String couponCode
    ) {
        Map<String, Object> res = new HashMap<>();
        String cleanPhone = sanitizePhoneNumber(phoneNumber);
        String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";
        com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
        if (user == null) {
            user = new com.saipraveen.login_registration.entity.User();
            user.setName("Student (+91 " + cleanPhone + ")");
            user.setEmail(waEmail);
            user.setPassword("WA_BOT_USER_NOPASS");
            user.setWalletBalance(0.0);
            user.setReferralCode("WA_" + cleanPhone);
            user = userRepository.save(user);
        }

        if (couponCode == null || couponCode.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "Coupon code is required");
            return ResponseEntity.ok(res);
        }

        com.saipraveen.login_registration.entity.Coupon coupon = couponRepository.findByCouponCodeIgnoreCase(couponCode.trim());
        if (coupon == null || Boolean.FALSE.equals(coupon.getActive())) {
            res.put("success", false);
            res.put("message", "Invalid or inactive coupon code.");
            return ResponseEntity.ok(res);
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            coupon.setActive(false);
            couponRepository.save(coupon);
            res.put("success", false);
            res.put("message", "Coupon code has expired.");
            return ResponseEntity.ok(res);
        }

        if (coupon.getMaxUses() != null && coupon.getMaxUses() <= 0) {
            coupon.setActive(false);
            couponRepository.save(coupon);
            res.put("success", false);
            res.put("message", "Coupon code has reached maximum uses.");
            return ResponseEntity.ok(res);
        }

        double credit = coupon.getDiscountAmount() != null ? coupon.getDiscountAmount() : 10.0;
        double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        double newBalance = currentBalance + credit;
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        if (coupon.getMaxUses() != null) {
            coupon.setMaxUses(coupon.getMaxUses() - 1);
            if (coupon.getMaxUses() <= 0) {
                coupon.setActive(false);
            }
            couponRepository.save(coupon);
        }

        res.put("success", true);
        res.put("creditedAmount", credit);
        res.put("newBalance", newBalance);
        res.put("message", "Coupon redeemed successfully! ₹" + String.format("%.2f", credit) + " added to your wallet.");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/pay-via-wallet")
    public ResponseEntity<?> payViaWallet(
            @RequestParam String orderId,
            @RequestParam String phoneNumber
    ) {
        Map<String, Object> res = new HashMap<>();
        String cleanPhone = sanitizePhoneNumber(phoneNumber);
        String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";
        com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
        if (user == null) {
            res.put("success", false);
            res.put("message", "User account not found");
            return ResponseEntity.ok(res);
        }

        PdfFile pdf = pdfFileService.getOrderByOrderId(orderId);
        if (pdf == null) {
            res.put("success", false);
            res.put("message", "Order not found");
            return ResponseEntity.ok(res);
        }

        if ("PAID".equals(pdf.getPaymentStatus())) {
            res.put("success", true);
            res.put("alreadyPaid", true);
            res.put("otp", pdf.getOtpCode());
            res.put("message", "Order already paid");
            return ResponseEntity.ok(res);
        }

        double price = pdf.getPrice() != null ? pdf.getPrice() : 0.0;
        double userBal = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;

        if (userBal < price) {
            res.put("success", false);
            res.put("message", "Insufficient wallet balance (Available: ₹" + String.format("%.2f", userBal) + ", Required: ₹" + String.format("%.2f", price) + ")");
            return ResponseEntity.ok(res);
        }

        user.setWalletBalance(userBal - price);
        userRepository.save(user);

        pdfFileService.markAsPaid(orderId, "WALLET_PAYMENT");

        PdfFile updatedPdf = pdfFileService.getOrderByOrderId(orderId);
        String otp = updatedPdf != null ? updatedPdf.getOtpCode() : pdf.getOtpCode();

        res.put("success", true);
        res.put("orderId", orderId);
        res.put("otp", otp);
        res.put("amountPaid", price);
        res.put("newBalance", user.getWalletBalance());
        res.put("message", "Payment successful via Wallet!");
        return ResponseEntity.ok(res);
    }

    @org.springframework.web.bind.annotation.GetMapping("/webhook/whatsapp")
    public ResponseEntity<?> verifyWhatsAppWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.challenge", required = false) String challenge,
            @RequestParam(name = "hub.verify_token", required = false) String token
    ) {
        if ("subscribe".equalsIgnoreCase(mode) && "cloudprint_token".equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.ok(challenge != null ? challenge : "WhatsApp Webhook Active");
    }

    @PostMapping("/webhook/whatsapp")
    public ResponseEntity<?> whatsappWebhook(@RequestBody Map<String, Object> payload) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", "WhatsApp message received successfully");
        return ResponseEntity.ok(res);
    }
}
