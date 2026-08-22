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
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private UserService userService;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @RequestParam Long userId
    ) {
        try {
            Double balance = userService.getWalletBalance(userId);
            return ResponseEntity.ok(balance != null ? balance : 0.0);
        } catch (Exception e) {
            System.err.println("Notice: user wallet balance lookup for userId " + userId + ": " + e.getMessage());
            return ResponseEntity.ok(0.0);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addBalance(
            @RequestParam Long userId,
            @RequestParam Double amount,
            @RequestParam(required = false) String paymentId
    ) {
        try {
            if (userId == null || amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body("Invalid user or amount");
            }
            User user = userService.creditWallet(userId, amount);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("walletBalance", user.getWalletBalance());
            res.put("message", "Wallet credited successfully with ₹" + amount);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
