package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
