package com.saipraveen.login_registration.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.service.UserService;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(originPatterns = "*")
public class WalletController {

    @Autowired
    private UserService userService;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber
    ) {
        try {
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            } else if (email != null && !email.trim().isEmpty()) {
                user = userService.findByEmail(email.trim());
            } else if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
                if (cleanPhone.length() > 10) cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
                user = userService.findByEmail("wa_" + cleanPhone + "@whatsapp.cloudprint");
            }
            if (user != null) {
                Double balance = user.getWalletBalance();
                return ResponseEntity.ok(balance != null ? balance : 0.0);
            }
            return ResponseEntity.ok(0.0);
        } catch (Exception e) {
            System.err.println("Notice: user wallet balance lookup: " + e.getMessage());
            return ResponseEntity.ok(0.0);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addBalance(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam Double amount,
            @RequestParam(required = false) String paymentId
    ) {
        try {
            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body("Invalid amount");
            }
            User user = null;
            if (userId != null) {
                user = userService.getUserById(userId);
            } else if (email != null && !email.trim().isEmpty()) {
                user = userService.findByEmail(email.trim());
            } else if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
                if (cleanPhone.length() > 10) cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
                user = userService.findByEmail("wa_" + cleanPhone + "@whatsapp.cloudprint");
            }
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            User updated = userService.creditWallet(user.getId(), amount);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("userId", updated.getId());
            res.put("walletBalance", updated.getWalletBalance());
            res.put("message", "Wallet credited successfully with ₹" + amount);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
