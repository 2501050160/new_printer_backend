package com.saipraveen.login_registration.controller;

import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.CollegeConfig;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.CollegeConfigRepository;
import com.saipraveen.login_registration.service.PdfFileService;
import com.saipraveen.login_registration.service.UserService;

@RestController
@RequestMapping("/api/webhook")
@CrossOrigin(originPatterns = "*") // Allow Razorpay to POST from any origin
public class RazorpayWebhookController {

    @Autowired
    private PdfFileService pdfService;

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private CampusBlockRepository campusBlockRepository;

    @Autowired
    private CollegeConfigRepository collegeConfigRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private com.saipraveen.login_registration.service.SseService sseService;

    // Razorpay default secret from application.properties
    @Value("${razorpay.key.secret:zth9Qce6MLqEs6O151FPfIsV}")
    private String defaultRazorpayKeySecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Razorpay webhook endpoint.
     * Verifies the signature and updates payment status based on the event.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handle(@RequestHeader("X-Razorpay-Signature") String signature,
                                         @RequestBody String payload) {
        try {
            JSONObject event = new JSONObject(payload);
            String eventName = event.getString("event");
            JSONObject payment = event.getJSONObject("payload")
                                      .getJSONObject("payment")
                                      .getJSONObject("entity");
            
            // Retrieve application order ID from payment notes, falling back to Razorpay order_id
            String orderId = null;
            if (payment.has("notes") && !payment.isNull("notes")) {
                JSONObject notes = payment.getJSONObject("notes");
                if (notes.has("app_order_id")) {
                    orderId = notes.getString("app_order_id");
                } else if (notes.has("orderId")) {
                    orderId = notes.getString("orderId");
                }
            }
            if (orderId == null && payment.has("order_id") && !payment.isNull("order_id")) {
                orderId = payment.getString("order_id");
            }
            
            if (orderId == null) {
                return new ResponseEntity<>("Order ID not found in webhook notes", HttpStatus.BAD_REQUEST);
            }

            // Lookup order and resolve College-level webhook secret if configured
            String currentSecret = (defaultRazorpayKeySecret != null && !defaultRazorpayKeySecret.trim().isEmpty())
                    ? defaultRazorpayKeySecret.trim()
                    : "zth9Qce6MLqEs6O151FPfIsV";
            PdfFile pdf = pdfFileRepository.findByOrderId(orderId);
            if (pdf != null && pdf.getBlockLocation() != null) {
                CampusBlock block = campusBlockRepository.findByName(pdf.getBlockLocation());
                if (block != null && block.getCollege() != null) {
                    CollegeConfig college = collegeConfigRepository.findByCollegeName(block.getCollege());
                    if (college != null && college.getRazorpayKeySecret() != null && !college.getRazorpayKeySecret().trim().isEmpty()) {
                        currentSecret = college.getRazorpayKeySecret().trim();
                    }
                }
            }
            if (currentSecret == null || currentSecret.trim().isEmpty()) {
                currentSecret = "zth9Qce6MLqEs6O151FPfIsV";
            }
            
            // Verify the incoming signature with the resolved secret
            if (!verifySignature(payload, signature, currentSecret)) {
                return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
            }
            
            String status = payment.getString("status"); // e.g., "captured", "failed"

            // ---- Wallet top-up: credit the user's wallet balance ----
            if (orderId.startsWith("WALLET_")) {
                if ("captured".equals(status)) {
                    // Resolve userId from Razorpay order notes
                    String userIdStr = null;
                    if (payment.has("notes") && !payment.isNull("notes")) {
                        JSONObject notes = payment.getJSONObject("notes");
                        if (notes.has("user_id")) {
                            userIdStr = notes.getString("user_id");
                        }
                    }
                    if (userIdStr != null && !userIdStr.isBlank()) {
                        try {
                            long uid = Long.parseLong(userIdStr.trim());
                            double amountRupees = payment.getDouble("amount") / 100.0;
                            String paymentId = payment.getString("id");
                            userService.creditWallet(uid, amountRupees);
                            System.out.println("[Webhook] Wallet credited: userId=" + uid + ", amount=" + amountRupees + ", paymentId=" + paymentId);
                        } catch (Exception walletErr) {
                            System.err.println("[Webhook] Failed to credit wallet: " + walletErr.getMessage());
                        }
                    } else {
                        System.err.println("[Webhook] WALLET_ order missing user_id in notes: " + orderId);
                    }
                }
                return new ResponseEntity<>("Wallet webhook processed", HttpStatus.OK);
            }

            // ---- PDF print order payment handling ----
            if ("captured".equals(status)) {
                String paymentId = payment.getString("id");
                pdfService.markAsPaid(orderId, paymentId);
                sseService.broadcastOrderEvent(orderId, "PAID");
                sseService.broadcastQueueEvent("Payment confirmed for order " + orderId);
            } else {
                pdfService.updatePaymentStatusByOrderId(orderId, status.toUpperCase());
                sseService.broadcastOrderEvent(orderId, status.toUpperCase());
            }

            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (Exception e) {
            // In production, log the error details
            return new ResponseEntity<>("Error processing webhook", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify Razorpay signature using HMAC SHA256 and Base64 encoding.
     */
    private boolean verifySignature(String payload, String receivedSignature, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), HMAC_SHA256);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(payload.getBytes());
            // Razorpay uses Hex encoding for its signature, but our webhook code was using Base64.
            // If Razorpay requires Hex, we should compute Hex. However, sticking to existing codebase if it was working:
            // But actually Razorpay documentation specifies Hex string encoding.
            // Let's implement Hex encoding here safely.
            StringBuilder hexString = new StringBuilder(2 * digest.length);
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String generatedHex = hexString.toString();
            
            // Backwards compatible check just in case Base64 was historically used
            String generatedBase64 = Base64.getEncoder().encodeToString(digest);
            
            return generatedHex.equals(receivedSignature) || generatedBase64.equals(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
