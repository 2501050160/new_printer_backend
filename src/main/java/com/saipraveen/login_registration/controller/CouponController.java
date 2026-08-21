package com.saipraveen.login_registration.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleException(Throwable t) {
        t.printStackTrace();
        String msg = t.getMessage() != null ? t.getMessage() : t.toString();
        return ResponseEntity.status(500).body(Map.of("error", msg, "class", t.getClass().getName()));
    }

    private Map<String, Object> mapCoupon(Coupon c) {
        if (c == null) return Collections.emptyMap();
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("couponCode", c.getCouponCode());
        m.put("discountPercentage", c.getDiscountPercentage() != null ? c.getDiscountPercentage() : 0.0);
        m.put("discountAmount", c.getDiscountAmount() != null ? c.getDiscountAmount() : 0.0);
        m.put("minOrderAmount", c.getMinOrderAmount() != null ? c.getMinOrderAmount() : 0.0);
        m.put("expiryDate", c.getExpiryDate() != null ? c.getExpiryDate().toString() : null);
        m.put("maxUses", c.getMaxUses() != null ? c.getMaxUses() : 100);
        m.put("usedCount", c.getUsedCount() != null ? c.getUsedCount() : 0);
        m.put("active", c.getActive() != null ? c.getActive() : true);
        return m;
    }

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
                if (body.get("minOrderAmount") != null) {
                    try {
                        coupon.setMinOrderAmount(Double.parseDouble(body.get("minOrderAmount").toString()));
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
                    String dateStr = body.get("expiryDate").toString().trim();
                    if (dateStr.contains("T")) {
                        dateStr = dateStr.split("T")[0];
                    }
                    coupon.setExpiryDate(dateStr);
                } else {
                    coupon.setExpiryDate(LocalDate.now().plusDays(30).toString());
                }
            }
            Coupon saved = service.createCoupon(coupon);
            return ResponseEntity.ok(mapCoupon(saved));
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Error in createCoupon: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString(), "class", e.getClass().getName()));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> createRefundCoupon(
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String code
    ) {
        try {
            Coupon coupon = new Coupon();
            String couponCode = (code != null && !code.trim().isEmpty()) 
                ? code.trim().toUpperCase() 
                : String.valueOf(100000 + new java.util.Random().nextInt(900000));
            coupon.setCouponCode(couponCode);
            coupon.setDiscountAmount(amount != null && amount > 0 ? amount : 2.0);
            coupon.setDiscountPercentage(0.0);
            coupon.setExpiryDate(LocalDate.now().plusDays(7).toString());
            coupon.setMaxUses(1);
            coupon.setUsedCount(0);
            coupon.setActive(true);
            Coupon saved = service.createCoupon(coupon);
            return ResponseEntity.ok(mapCoupon(saved));
        } catch (Throwable e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getCoupons() {
        try {
            List<Coupon> raw = service.getAllCoupons();
            List<Map<String, Object>> result = new ArrayList<>();
            if (raw != null) {
                for (Coupon c : raw) {
                    result.add(mapCoupon(c));
                }
            }
            return ResponseEntity.ok(result);
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Error in getCoupons: " + e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        try {
            Coupon c = service.validateCoupon(couponCode);
            return ResponseEntity.ok(mapCoupon(c));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid coupon"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        try {
            Coupon c = service.useCoupon(couponCode);
            return ResponseEntity.ok(mapCoupon(c));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to use coupon"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        try {
            service.deleteCoupon(id);
            return ResponseEntity.ok(Map.of("message", "Coupon Deleted"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to delete coupon"));
        }
    }
}