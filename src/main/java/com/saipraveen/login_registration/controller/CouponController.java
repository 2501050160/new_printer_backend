package com.saipraveen.login_registration.controller;

import java.util.ArrayList;
import java.util.List;
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
import com.saipraveen.login_registration.repository.CouponRepository;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin(origins = "*")
public class CouponController {

    @Autowired
    private CouponRepository couponRepository;

    @GetMapping("/all")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        try {
            List<Coupon> list = couponRepository.findAll();
            return ResponseEntity.ok(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@RequestBody(required = false) Coupon coupon) {
        try {
            if (coupon == null) {
                coupon = new Coupon();
            }
            if (coupon.getCouponCode() == null || coupon.getCouponCode().trim().isEmpty()) {
                coupon.setCouponCode("PRINT" + (1000 + new java.util.Random().nextInt(9000)));
            } else {
                coupon.setCouponCode(coupon.getCouponCode().trim().toUpperCase());
            }

            // Check if coupon code already exists to update it
            List<Coupon> existingList = couponRepository.findByCouponCodeIgnoreCase(coupon.getCouponCode());
            if (existingList != null && !existingList.isEmpty()) {
                Coupon existing = existingList.get(0);
                if (coupon.getDiscountPercentage() != null) existing.setDiscountPercentage(coupon.getDiscountPercentage());
                if (coupon.getDiscountAmount() != null) existing.setDiscountAmount(coupon.getDiscountAmount());
                if (coupon.getMinOrderAmount() != null) existing.setMinOrderAmount(coupon.getMinOrderAmount());
                if (coupon.getExpiryDate() != null) existing.setExpiryDate(coupon.getExpiryDate());
                if (coupon.getMaxUses() != null) existing.setMaxUses(coupon.getMaxUses());
                if (coupon.getActive() != null) existing.setActive(coupon.getActive());
                return ResponseEntity.ok(couponRepository.save(existing));
            }

            if (coupon.getDiscountPercentage() == null && coupon.getDiscountAmount() == null) {
                coupon.setDiscountPercentage(10.0);
            }
            if (coupon.getMinOrderAmount() == null) coupon.setMinOrderAmount(0.0);
            if (coupon.getDiscountAmount() == null) coupon.setDiscountAmount(0.0);
            if (coupon.getDiscountPercentage() == null) coupon.setDiscountPercentage(0.0);
            if (coupon.getExpiryDate() == null || coupon.getExpiryDate().trim().isEmpty()) {
                coupon.setExpiryDate(java.time.LocalDate.now().plusDays(30).toString());
            }
            if (coupon.getMaxUses() == null || coupon.getMaxUses() < 1) coupon.setMaxUses(100);
            if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
            if (coupon.getActive() == null) coupon.setActive(true);

            return ResponseEntity.ok(couponRepository.save(coupon));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to create coupon"));
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
            coupon.setMinOrderAmount(0.0);
            coupon.setExpiryDate(java.time.LocalDate.now().plusDays(7).toString());
            coupon.setMaxUses(1);
            coupon.setUsedCount(0);
            coupon.setActive(true);
            return ResponseEntity.ok(couponRepository.save(coupon));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Code is required"));
            }
            String cleanCode = couponCode.trim().toUpperCase();
            List<Coupon> list = couponRepository.findByCouponCodeIgnoreCase(cleanCode);
            if (list == null || list.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Not Found"));
            }
            Coupon coupon = list.get(0);
            if (Boolean.FALSE.equals(coupon.getActive())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Disabled"));
            }
            return ResponseEntity.ok(coupon);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid coupon"));
        }
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        try {
            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Code is required"));
            }
            String cleanCode = couponCode.trim().toUpperCase();
            List<Coupon> list = couponRepository.findByCouponCodeIgnoreCase(cleanCode);
            if (list == null || list.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Coupon Not Found"));
            }
            Coupon coupon = list.get(0);
            int used = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            coupon.setUsedCount(used + 1);
            if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
                coupon.setActive(false);
            }
            return ResponseEntity.ok(couponRepository.save(coupon));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to use coupon"));
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        try {
            if (id != null) {
                couponRepository.deleteById(id);
            }
            return ResponseEntity.ok(Map.of("message", "Coupon Deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to delete coupon"));
        }
    }
}