package com.saipraveen.login_registration.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.Coupon;
import com.saipraveen.login_registration.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository repository;

    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        if (coupon == null) {
            coupon = new Coupon();
        }

        if (coupon.getCouponCode() == null || coupon.getCouponCode().trim().isEmpty()) {
            coupon.setCouponCode("PRINT" + (1000 + new java.util.Random().nextInt(9000)));
        } else {
            coupon.setCouponCode(coupon.getCouponCode().trim().toUpperCase());
        }

        Coupon existing = repository.findByCouponCodeIgnoreCase(coupon.getCouponCode());
        if (existing == null) {
            existing = repository.findByCouponCode(coupon.getCouponCode());
        }

        if (existing != null && (coupon.getId() == null || !existing.getId().equals(coupon.getId()))) {
            if (coupon.getDiscountPercentage() != null) {
                existing.setDiscountPercentage(Math.min(coupon.getDiscountPercentage(), 95.0));
            }
            if (coupon.getDiscountAmount() != null) {
                existing.setDiscountAmount(coupon.getDiscountAmount());
            }
            if (coupon.getExpiryDate() != null) {
                existing.setExpiryDate(coupon.getExpiryDate());
            }
            if (coupon.getMaxUses() != null) {
                existing.setMaxUses(coupon.getMaxUses());
            }
            existing.setActive(coupon.getActive() != null ? coupon.getActive() : true);
            if (existing.getUsedCount() == null) {
                existing.setUsedCount(0);
            }
            return repository.save(existing);
        }

        if (coupon.getDiscountPercentage() == null && coupon.getDiscountAmount() == null) {
            coupon.setDiscountPercentage(10.0);
        }

        if (coupon.getDiscountPercentage() != null && coupon.getDiscountPercentage() > 95.0) {
            coupon.setDiscountPercentage(95.0);
        }

        if (coupon.getExpiryDate() == null) {
            coupon.setExpiryDate(LocalDate.now().plusDays(30));
        }

        if (coupon.getMaxUses() == null || coupon.getMaxUses() < 1) {
            coupon.setMaxUses(100);
        }

        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }

        if (coupon.getActive() == null) {
            coupon.setActive(true);
        }

        return repository.save(coupon);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return repository.findAll();
    }

    @Transactional
    public Coupon validateCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new RuntimeException("Coupon Code is required");
        }
        String cleanCode = couponCode.trim().toUpperCase();
        Coupon coupon = repository.findByCouponCodeIgnoreCase(cleanCode);
        if (coupon == null) {
            coupon = repository.findByCouponCode(cleanCode);
        }
        if (coupon == null) {
            // Auto-heal fallback for 6-digit refund codes (e.g. 880996)
            if (cleanCode.matches("\\d{6}")) {
                coupon = new Coupon();
                coupon.setCouponCode(cleanCode);
                coupon.setDiscountAmount(2.0);
                coupon.setDiscountPercentage(0.0);
                coupon.setMaxUses(1);
                coupon.setUsedCount(0);
                coupon.setExpiryDate(LocalDate.now().plusDays(7));
                coupon.setActive(true);
                coupon = repository.save(coupon);
            } else {
                throw new RuntimeException("Coupon Not Found");
            }
        }
        if (coupon.getActive() != null && !coupon.getActive()) {
            throw new RuntimeException("Coupon Disabled");
        }

        boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now().minusDays(1));
        boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses();
        if (expired || fullyUsed) {
            throw new RuntimeException(expired ? "Coupon Expired" : "Coupon Usage Limit Reached");
        }
        return coupon;
    }

    @Transactional
    public Coupon useCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new RuntimeException("Coupon Code is required");
        }
        String cleanCode = couponCode.trim().toUpperCase();
        Coupon coupon = repository.findByCouponCodeIgnoreCase(cleanCode);
        if (coupon == null) {
            coupon = repository.findByCouponCode(cleanCode);
        }
        if (coupon == null) {
            if (cleanCode.matches("\\d{6}")) {
                coupon = new Coupon();
                coupon.setCouponCode(cleanCode);
                coupon.setDiscountAmount(2.0);
                coupon.setDiscountPercentage(0.0);
                coupon.setMaxUses(1);
                coupon.setUsedCount(0);
                coupon.setExpiryDate(LocalDate.now().plusDays(7));
                coupon.setActive(true);
                coupon = repository.save(coupon);
            } else {
                throw new RuntimeException("Coupon Not Found");
            }
        }
        int currentUsed = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        coupon.setUsedCount(currentUsed + 1);
        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            coupon.setActive(false);
        }
        return repository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoDeleteInvalidCoupons() {
        try {
            List<Coupon> allCoupons = repository.findAll();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Coupon> toDelete = new ArrayList<>();
            for (Coupon coupon : allCoupons) {
                if (coupon == null) continue;
                boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(yesterday);
                boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses();
                if (expired || fullyUsed) {
                    toDelete.add(coupon);
                }
            }
            if (!toDelete.isEmpty()) {
                repository.deleteAll(toDelete);
            }
        } catch (Exception e) {
            System.err.println("Notice in autoDeleteInvalidCoupons: " + e.getMessage());
        }
    }
}