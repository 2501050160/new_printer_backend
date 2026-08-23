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
    private com.saipraveen.login_registration.repository.PdfFileRepository pdfFileRepository;

    @Autowired
    private com.saipraveen.login_registration.service.PrinterConfigService printerConfigService;

    @Autowired
    private com.saipraveen.login_registration.repository.UserRepository userRepository;

    @Autowired
    private com.saipraveen.login_registration.repository.CampusBlockRepository campusBlockRepository;

    @Autowired
    private com.saipraveen.login_registration.service.PricingService pricingService;

    @Autowired
    private com.saipraveen.login_registration.service.SystemSettingService systemSettingService;

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

            // Determine campus/college from kiosk location
            String college = "KLU";
            if (blockLocation != null && !blockLocation.isEmpty()) {
                com.saipraveen.login_registration.entity.CampusBlock blk = campusBlockRepository.findByName(blockLocation);
                if (blk != null && blk.getCollege() != null) {
                    college = blk.getCollege();
                }
            }

            com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
            if (user == null) {
                try {
                    user = userRepository.findByReferralCode("WA_" + cleanPhone);
                } catch (Exception ignored) {}
            }
            if (user == null) {
                user = new com.saipraveen.login_registration.entity.User();
                user.setName(fullNameWithPhone);
                user.setEmail(waEmail);
                user.setPassword("WA_BOT_USER_NOPASS");
                user.setWalletBalance(0.0);
                user.setCollege(college);
                user.setReferralCode("WA_" + cleanPhone + "_" + (System.currentTimeMillis() % 1000));
                user.setBlocked(false);
                try {
                    user = userRepository.save(user);
                } catch (Exception ex) {
                    System.err.println("User creation fallback for " + waEmail + ": " + ex.getMessage());
                    user = userRepository.findByEmail(waEmail);
                    if (user == null) {
                        user = userRepository.findAll().stream()
                            .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().contains(cleanPhone.toLowerCase()))
                            .findFirst().orElse(null);
                    }
                }
            } else {
                if (Boolean.TRUE.equals(user.getBlocked())) {
                    return ResponseEntity.badRequest().body("⛔ Account Suspended: Your WhatsApp number has been blocked by the administrator. Please contact campus admin to unblock your account.");
                }
                boolean changed = false;
                if (user.getName() == null || !user.getName().contains("+91 " + cleanPhone)) {
                    user.setName(fullNameWithPhone);
                    changed = true;
                }
                if (user.getCollege() == null || !user.getCollege().equalsIgnoreCase(college)) {
                    user.setCollege(college);
                    changed = true;
                }
                if (changed) {
                    user = userRepository.save(user);
                }
            }

            // Save PDF to DB linked to the unique WhatsApp User ID
            PdfFile pdf = pdfFileService.savePdf(file, user.getId(), fullNameWithPhone, blockLocation);
            pdf.setOrderChannel("WHATSAPP");
            pdf = pdfFileRepository.save(pdf);

            // Update order details to generate real Order ID and calculate college-wise pricing
            PdfFile updated = pdfFileService.updateOrder(
                    pdf.getOrderId(),
                    copies != null ? copies : 1,
                    selectedPages != null ? selectedPages : "ALL",
                    printType,
                    blockLocation,
                    "1-up",
                    doubleSided != null ? doubleSided : false
            );
            updated.setOrderChannel("WHATSAPP");
            updated = pdfFileRepository.save(updated);

            // Calculate details from updated PDF
            int pages = updated.getTotalPages() != null ? updated.getTotalPages() : 1;
            Double rate = pricingService.getPrice(printType, blockLocation);
            if (rate == null || rate == 0.0) {
                rate = "COLOR".equalsIgnoreCase(printType) ? 5.0 : 2.0;
            }
            double estimatedTotal = updated.getPrice() != null ? updated.getPrice() : (pages * rate * (copies != null ? copies : 1));

            // Handle automatic full or partial wallet deduction
            double userBal = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
            boolean paidViaWallet = false;
            boolean partialWallet = false;
            double walletDeducted = 0.0;
            double finalPriceToPay = estimatedTotal;

            if (userBal >= estimatedTotal && estimatedTotal > 0) {
                // Full wallet payment
                user.setWalletBalance(userBal - estimatedTotal);
                userRepository.save(user);
                pdfFileService.markAsPaid(updated.getOrderId(), "WALLET_PAYMENT");
                paidViaWallet = true;
                finalPriceToPay = 0.0;
                walletDeducted = estimatedTotal;
            } else if (userBal > 0 && userBal < estimatedTotal) {
                // Partial wallet payment
                walletDeducted = userBal;
                user.setWalletBalance(0.0);
                userRepository.save(user);
                
                finalPriceToPay = estimatedTotal - walletDeducted;
                updated.setOriginalPrice(estimatedTotal);
                updated.setDiscountAmount(walletDeducted);
                updated.setPrice(finalPriceToPay);
                updated = pdfFileRepository.save(updated);
                partialWallet = true;
            }

            PdfFile latestPdf = pdfFileRepository.findByOrderId(updated.getOrderId());
            String realOtp = (latestPdf != null && latestPdf.getOtpCode() != null) ? latestPdf.getOtpCode() : (updated.getOtpCode() != null ? updated.getOtpCode() : "");
            String realOrderId = updated.getOrderId() != null ? updated.getOrderId() : pdf.getOrderId();
            String checkoutUrl = frontendUrl + "/pay?orderId=" + realOrderId;

            StringBuilder botMsg = new StringBuilder();
            botMsg.append("🖨️ *Cloud Print Order Created!*\n");
            botMsg.append("-----------------------------\n");
            botMsg.append("📄 *File*: ").append(file.getOriginalFilename()).append("\n");
            botMsg.append("🏫 *Campus*: ").append(college).append(" (").append(blockLocation).append(")\n");
            botMsg.append("📊 *Pages*: ").append(pages).append(" | *Print Type*: ").append(printType).append("\n");
            if (updated.getDiscountAmount() != null && updated.getDiscountAmount() > 0) {
                botMsg.append("🏷️ *Discount / Wallet Applied*: -₹").append(String.format("%.2f", updated.getDiscountAmount())).append("\n");
            }
            botMsg.append("💰 *Total Amount*: ₹").append(String.format("%.2f", estimatedTotal)).append("\n");
            if (paidViaWallet) {
                botMsg.append("✅ *Payment*: Paid in Full via Wallet Balance\n");
            } else if (partialWallet) {
                botMsg.append("💳 *Remaining to Pay*: ₹").append(String.format("%.2f", finalPriceToPay)).append("\n");
            }
            botMsg.append("📺 *Release OTP*: Displayed on *").append(blockLocation).append(" TV Display Panel*\n");
            botMsg.append("📍 *Target Kiosk*: ").append(blockLocation).append("\n\n");
            if (!paidViaWallet) {
                botMsg.append("👉 *Complete Payment*: ").append(checkoutUrl);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", realOrderId);
            response.put("otp", realOtp);
            response.put("totalPages", pages);
            response.put("college", college);
            response.put("ratePerPage", rate);
            response.put("discountAmount", updated.getDiscountAmount() != null ? updated.getDiscountAmount() : 0.0);
            response.put("estimatedTotal", estimatedTotal);
            response.put("finalPriceToPay", finalPriceToPay);
            response.put("paidViaWallet", paidViaWallet);
            response.put("partialWallet", partialWallet);
            response.put("walletDeducted", walletDeducted);
            response.put("newBalance", user.getWalletBalance());
            response.put("blockLocation", blockLocation);
            response.put("paymentUrl", checkoutUrl);
            response.put("botMessage", botMsg.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Direct Upload Exception: " + e.getMessage());
            return ResponseEntity.badRequest().body("Failed to process order: " + e.getMessage());
        }
    }

    @Autowired
    private com.saipraveen.login_registration.repository.CouponRepository couponRepository;

    @org.springframework.web.bind.annotation.GetMapping("/user-balance")
    public ResponseEntity<?> getUserBalance(@RequestParam String phoneNumber) {
        String cleanPhone = sanitizePhoneNumber(phoneNumber);
        String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";
        com.saipraveen.login_registration.entity.User user = userRepository.findByEmail(waEmail);
        Map<String, Object> res = new HashMap<>();
        if (user != null && Boolean.TRUE.equals(user.getBlocked())) {
            res.put("phoneNumber", cleanPhone);
            res.put("balance", 0.0);
            res.put("blocked", true);
            res.put("message", "⛔ Account Suspended: Your account is blocked by the administrator.");
            return ResponseEntity.ok(res);
        }
        double balance = (user != null && user.getWalletBalance() != null) ? user.getWalletBalance() : 0.0;
        res.put("phoneNumber", cleanPhone);
        res.put("balance", balance);
        res.put("blocked", false);
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
            user.setBlocked(false);
            user = userRepository.save(user);
        } else if (Boolean.TRUE.equals(user.getBlocked())) {
            res.put("success", false);
            res.put("message", "⛔ Account Suspended: Your account has been blocked by the administrator.");
            return ResponseEntity.ok(res);
        }

        if (couponCode == null || couponCode.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "Coupon code is required");
            return ResponseEntity.ok(res);
        }

        String cleanCode = couponCode.trim().toUpperCase();
        java.util.List<com.saipraveen.login_registration.entity.Coupon> coupons = couponRepository.findByCouponCodeIgnoreCase(cleanCode);
        com.saipraveen.login_registration.entity.Coupon coupon = (coupons != null && !coupons.isEmpty()) ? coupons.get(0) : null;
        if (coupon == null) {
            // Auto-heal fallback for 6-digit refund codes (e.g. 880996)
            if (cleanCode.matches("\\d{6}")) {
                coupon = new com.saipraveen.login_registration.entity.Coupon();
                coupon.setCouponCode(cleanCode);
                double detectedAmount = 2.0;
                try {
                    java.util.List<com.saipraveen.login_registration.entity.PdfFile> recentOrders = pdfFileRepository.findByUserId(user.getId());
                    if (recentOrders != null && !recentOrders.isEmpty()) {
                        for (com.saipraveen.login_registration.entity.PdfFile p : recentOrders) {
                            if ("CANCELLED".equalsIgnoreCase(p.getStatus()) || "EXPIRED".equalsIgnoreCase(p.getStatus()) || "PAID".equalsIgnoreCase(p.getPaymentStatus())) {
                                if (p.getPrice() != null && p.getPrice() > 0) {
                                    detectedAmount = p.getPrice();
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                coupon.setDiscountAmount(detectedAmount);
                coupon.setDiscountPercentage(0.0);
                coupon.setMaxUses(1);
                coupon.setUsedCount(0);
                coupon.setExpiryDate(java.time.LocalDate.now().plusDays(7).toString());
                coupon.setActive(true);
                coupon = couponRepository.save(coupon);
            }
        }

        if (coupon == null || Boolean.FALSE.equals(coupon.getActive())) {
            res.put("success", false);
            res.put("message", "Invalid or inactive coupon code.");
            return ResponseEntity.ok(res);
        }

        boolean isExpired = false;
        if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().trim().isEmpty()) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(coupon.getExpiryDate().trim());
                if (date.isBefore(java.time.LocalDate.now())) {
                    isExpired = true;
                }
            } catch (Exception ignored) {}
        }

        if (isExpired) {
            coupon.setActive(false);
            couponRepository.save(coupon);
            res.put("success", false);
            res.put("message", "Coupon code has expired.");
            return ResponseEntity.ok(res);
        }

        if (coupon.getMaxUses() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            coupon.setActive(false);
            couponRepository.save(coupon);
            res.put("success", false);
            res.put("message", "Coupon code has reached maximum uses.");
            return ResponseEntity.ok(res);
        }

        double credit = (coupon.getDiscountAmount() != null && coupon.getDiscountAmount() > 0) ? coupon.getDiscountAmount() : 2.0;
        double currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        double newBalance = currentBalance + credit;
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            coupon.setActive(false);
        }
        couponRepository.save(coupon);

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
        if (Boolean.TRUE.equals(user.getBlocked())) {
            res.put("success", false);
            res.put("message", "⛔ Account Suspended: Your account has been blocked by the administrator.");
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

    @org.springframework.web.bind.annotation.GetMapping("/college-prices")
    public ResponseEntity<?> getCollegePrices(
            @RequestParam(defaultValue = "KLU") String college,
            @RequestParam(required = false) String blockLocation
    ) {
        String targetBlock = blockLocation;
        if (targetBlock == null || targetBlock.isEmpty()) {
            java.util.List<com.saipraveen.login_registration.entity.CampusBlock> blks = campusBlockRepository.findByCollege(college);
            if (blks != null && !blks.isEmpty()) {
                targetBlock = blks.get(0).getName();
            } else {
                targetBlock = "C Block";
            }
        }
        Double bwRate = pricingService.getPrice("BW", targetBlock);
        if (bwRate == null || bwRate == 0.0) bwRate = 2.0;
        Double colorRate = pricingService.getPrice("COLOR", targetBlock);
        if (colorRate == null || colorRate == 0.0) colorRate = 5.0;
        Double duplexRate = pricingService.getPrice("DUPLEX", targetBlock);
        if (duplexRate == null || duplexRate == 0.0) duplexRate = 2.0;

        boolean offpeakEnabled = systemSettingService.getSettingBool("offpeak_enabled_" + college, systemSettingService.getSettingBool("offpeak_enabled", true));
        double offpeakDiscountPercent = systemSettingService.getSettingDouble("offpeak_discount_percent_" + college, systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0));

        boolean thesisEnabled = systemSettingService.getSettingBool("thesis_enabled_" + college, systemSettingService.getSettingBool("thesis_enabled", true));
        double thesisDiscountPercent = systemSettingService.getSettingDouble("thesis_discount_percent_" + college, systemSettingService.getSettingDouble("thesis_discount_percent", 15.0));
        double thesisDiscountPages = systemSettingService.getSettingDouble("thesis_discount_pages_" + college, systemSettingService.getSettingDouble("thesis_discount_pages", 500.0));

        Map<String, Object> res = new HashMap<>();
        res.put("college", college);
        res.put("blockLocation", targetBlock);
        res.put("bwPricePerPage", bwRate);
        res.put("colorPricePerPage", colorRate);
        res.put("duplexPricePerPage", duplexRate);
        res.put("offpeakEnabled", offpeakEnabled);
        res.put("offpeakDiscountPercent", offpeakDiscountPercent);
        res.put("thesisEnabled", thesisEnabled);
        res.put("thesisDiscountPercent", thesisDiscountPercent);
        res.put("thesisDiscountPages", thesisDiscountPages);

        StringBuilder info = new StringBuilder();
        info.append("🏫 *Print Rates for ").append(college).append(" Campus*\n");
        info.append("-----------------------------\n");
        info.append("📄 *B&W Print*: ₹").append(String.format("%.2f", bwRate)).append("/page\n");
        info.append("🎨 *Color Print*: ₹").append(String.format("%.2f", colorRate)).append("/page\n");
        info.append("🔄 *Double-Sided (Duplex)*: ₹").append(String.format("%.2f", duplexRate)).append("/page\n");
        if (offpeakEnabled) {
            info.append("🌙 *Off-Peak Hours*: ").append(offpeakDiscountPercent).append("% OFF active during night/morning windows\n");
        }
        if (thesisEnabled) {
            info.append("📚 *Bulk/Thesis Discount*: ").append(thesisDiscountPercent).append("% OFF for orders ≥ ").append((int)thesisDiscountPages).append(" pages\n");
        }
        res.put("formattedMessage", info.toString());

        return ResponseEntity.ok(res);
    }
}
