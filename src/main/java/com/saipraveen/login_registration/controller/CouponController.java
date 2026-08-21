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
import com.saipraveen.login_registration.repository.CouponRepository;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin(origins = "*")
public class CouponController {

    @Autowired
    private CouponRepository repository;

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

            if (coupon.getCouponCode() == null || coupon.getCouponCode().trim().isEmpty()) {
                coupon.setCouponCode("PRINT" + (1000 + new java.util.Random().nextInt(9000)));
            }

            List<Coupon> existingList = repository.findByCouponCodeIgnoreCase(coupon.getCouponCode());
            if (existingList != null && !existingList.isEmpty()) {
                Coupon existing = existingList.get(0);
                if (coupon.getDiscountPercentage() != null) existing.setDiscountPercentage(coupon.getDiscountPercentage());
                if (coupon.getDiscountAmount() != null) existing.setDiscountAmount(coupon.getDiscountAmount());
                if (coupon.getMinOrderAmount() != null) existing.setMinOrderAmount(coupon.getMinOrderAmount());
                if (coupon.getExpiryDate() != null) existing.setExpiryDate(coupon.getExpiryDate());
                if (coupon.getMaxUses() != null) existing.setMaxUses(coupon.getMaxUses());
                existing.setActive(coupon.getActive() != null ? coupon.getActive() : true);
                Coupon saved = repository.save(existing);
                return ResponseEntity.ok(mapCoupon(saved));
            }

            if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
            if (coupon.getActive() == null) coupon.setActive(true);

            Coupon saved = repository.save(coupon);
            return ResponseEntity.ok(mapCoupon(saved));
        } catch (Throwable e) {
            e.printStackTrace();
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
            Coupon saved = repository.save(coupon);
            return ResponseEntity.ok(mapCoupon(saved));
        } catch (Throwable e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getCoupons() {
        try {
            List<Coupon> raw = repository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();
            if (raw != null) {
                for (Coupon c : raw) {
                    result.add(mapCoupon(c));
                }
            }
            return ResponseEntity.ok(result);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon code required"));
            }
            List<Coupon> list = repository.findByCouponCodeIgnoreCase(couponCode.trim());
            if (list == null || list.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Not Found"));
            }
            return ResponseEntity.ok(mapCoupon(list.get(0)));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid coupon"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon code required"));
            }
            List<Coupon> list = repository.findByCouponCodeIgnoreCase(couponCode.trim());
            if (list == null || list.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Not Found"));
            }
            Coupon c = list.get(0);
            c.setUsedCount((c.getUsedCount() != null ? c.getUsedCount() : 0) + 1);
            if (c.getMaxUses() != null && c.getUsedCount() >= c.getMaxUses()) {
                c.setActive(false);
            }
            Coupon saved = repository.save(c);
            return ResponseEntity.ok(mapCoupon(saved));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to use coupon"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        try {
            if (id != null) {
                repository.deleteById(id);
            }
            return ResponseEntity.ok(Map.of("message", "Coupon Deleted"));
        } catch (Throwable e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to delete coupon"));
        }
    }
}