package com.saipraveen.login_registration.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.Coupon;
import com.saipraveen.login_registration.service.CouponService;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin(origins = "*")
public class CouponController {

    @Autowired
    private CouponService service;

    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Coupon coupon = new Coupon();
            if (body != null) {
                if (body.get("id") != null) {
                    try {
                        coupon.setId(Long.parseLong(body.get("id").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("couponCode") != null) {
                    coupon.setCouponCode(body.get("couponCode").toString().trim().toUpperCase());
                }
                if (body.get("discountPercentage") != null) {
                    try {
                        coupon.setDiscountPercentage(Double.parseDouble(body.get("discountPercentage").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("discountAmount") != null) {
                    try {
                        coupon.setDiscountAmount(Double.parseDouble(body.get("discountAmount").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("maxUses") != null) {
                    try {
                        coupon.setMaxUses(Integer.parseInt(body.get("maxUses").toString()));
                    } catch (Exception ignored) {}
                }
                if (body.get("active") != null) {
                    coupon.setActive(Boolean.parseBoolean(body.get("active").toString()));
                }
                if (body.get("expiryDate") != null && !body.get("expiryDate").toString().trim().isEmpty()) {
                    try {
                        String dateStr = body.get("expiryDate").toString().trim();
                        if (dateStr.contains("T")) {
                            dateStr = dateStr.split("T")[0];
                        }
                        coupon.setExpiryDate(LocalDate.parse(dateStr));
                    } catch (Exception e) {
                        System.err.println("Warning: Invalid date format for coupon expiryDate: " + body.get("expiryDate"));
                        coupon.setExpiryDate(LocalDate.now().plusDays(30));
                    }
                }
            }
            Coupon saved = service.createCoupon(coupon);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in createCoupon: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to create coupon"));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getCoupons() {
        try {
            return ResponseEntity.ok(service.getAllCoupons());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in getCoupons: " + e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        try {
            return ResponseEntity.ok(service.validateCoupon(couponCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid coupon"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        try {
            return ResponseEntity.ok(service.useCoupon(couponCode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to use coupon"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        try {
            service.deleteCoupon(id);
            return ResponseEntity.ok(Map.of("message", "Coupon Deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to delete coupon"));
        }
    }
}