package com.saipraveen.login_registration.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
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
@CrossOrigin(origins = "http://localhost:5173")
public class CouponController {

    @Autowired
    private CouponRepository couponRepository;

    @GetMapping("/all")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@RequestBody Coupon coupon) {
        if (coupon.getCouponCode() == null || coupon.getCouponCode().trim().isEmpty()) {
            coupon.setCouponCode("PRINT" + (1000 + new java.util.Random().nextInt(9000)));
        } else {
            coupon.setCouponCode(coupon.getCouponCode().trim().toUpperCase());
        }

        // Check if coupon code already exists
        List<Coupon> existingList = couponRepository.findByCouponCodeIgnoreCase(coupon.getCouponCode());
        if (existingList != null && !existingList.isEmpty()) {
            return ResponseEntity.badRequest().body("Coupon with this code already exists");
        }

        if (coupon.getDiscountPercentage() == null && coupon.getDiscountAmount() == null) {
            coupon.setDiscountPercentage(10.0);
        }
        if (coupon.getDiscountPercentage() == null) coupon.setDiscountPercentage(0.0);
        if (coupon.getDiscountAmount() == null) coupon.setDiscountAmount(0.0);
        if (coupon.getMinOrderAmount() == null) coupon.setMinOrderAmount(0.0);
        if (coupon.getExpiryDate() == null || coupon.getExpiryDate().trim().isEmpty()) {
            coupon.setExpiryDate(LocalDate.now().plusDays(30).toString());
        }
        if (coupon.getMaxUses() == null || coupon.getMaxUses() < 1) coupon.setMaxUses(100);
        if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
        if (coupon.getActive() == null) coupon.setActive(true);

        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/refund")
    public ResponseEntity<?> createRefundCoupon(
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String code
    ) {
        Coupon coupon = new Coupon();
        String couponCode = (code != null && !code.trim().isEmpty())
            ? code.trim().toUpperCase()
            : String.valueOf(100000 + new java.util.Random().nextInt(900000));
        coupon.setCouponCode(couponCode);
        coupon.setDiscountAmount(amount != null && amount > 0 ? amount : 2.0);
        coupon.setDiscountPercentage(0.0);
        coupon.setMinOrderAmount(0.0);
        coupon.setExpiryDate(LocalDate.now().plusDays(7).toString());
        coupon.setMaxUses(1);
        coupon.setUsedCount(0);
        coupon.setActive(true);
        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestParam String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Coupon code is required");
        }
        String cleanCode = couponCode.trim().toUpperCase();
        List<Coupon> list = couponRepository.findByCouponCodeIgnoreCase(cleanCode);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.badRequest().body("Coupon not found");
        }
        Coupon coupon = list.get(0);
        if (Boolean.FALSE.equals(coupon.getActive())) {
            return ResponseEntity.badRequest().body("Coupon is disabled");
        }
        return ResponseEntity.ok(coupon);
    }

    @PostMapping("/use")
    public ResponseEntity<?> useCoupon(@RequestParam String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Coupon code is required");
        }
        String cleanCode = couponCode.trim().toUpperCase();
        List<Coupon> list = couponRepository.findByCouponCodeIgnoreCase(cleanCode);
        if (list == null || list.isEmpty()) {
            return ResponseEntity.badRequest().body("Coupon not found");
        }
        Coupon coupon = list.get(0);
        int used = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        coupon.setUsedCount(used + 1);
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            coupon.setActive(false);
        }
        couponRepository.save(coupon);
        return ResponseEntity.ok(coupon);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteCoupon(@RequestParam Long id) {
        if (couponRepository.existsById(id)) {
            couponRepository.deleteById(id);
            return ResponseEntity.ok("Coupon deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoDeleteExpiredCoupons() {
        List<Coupon> allCoupons = couponRepository.findAll();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Coupon> toDelete = new ArrayList<>();
        for (Coupon coupon : allCoupons) {
            boolean expired = false;
            if (coupon.getExpiryDate() != null && !coupon.getExpiryDate().trim().isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(coupon.getExpiryDate().trim());
                    if (date.isBefore(yesterday)) expired = true;
                } catch (Exception ignored) {}
            }
            boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null
                    && coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses();
            if (expired || fullyUsed) {
                toDelete.add(coupon);
            }
        }
        if (!toDelete.isEmpty()) {
            couponRepository.deleteAll(toDelete);
            System.out.println("Auto-deleted " + toDelete.size() + " expired/used coupons");
        }
    }
}