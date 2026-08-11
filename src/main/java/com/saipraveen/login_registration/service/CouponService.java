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
            coupon.setCouponCode(String.valueOf(100000 + new java.util.Random().nextInt(900000)));
        } else {
            coupon.setCouponCode(coupon.getCouponCode().trim());
        }

        if (coupon.getDiscountPercentage() == null && coupon.getDiscountAmount() == null) {
            coupon.setDiscountPercentage(100.0);
        }

        if (coupon.getDiscountPercentage() != null && coupon.getDiscountPercentage() > 100.0) {
            coupon.setDiscountPercentage(100.0);
        }

        if (coupon.getExpiryDate() == null) {
            coupon.setExpiryDate(LocalDate.now().plusDays(7));
        }

        if (coupon.getMaxUses() == null) {
            coupon.setMaxUses(1);
        }

        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }

        if (coupon.getActive() == null) {
            coupon.setActive(true);
        }

        return repository.save(coupon);
    }

    @Transactional
    public List<Coupon> getAllCoupons() {
        autoDeleteInvalidCoupons();
        try {
            return repository.findAll();
        } catch (Exception e) {
            System.err.println("Error fetching all coupons: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Coupon validateCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new RuntimeException("Coupon Code is required");
        }

        String cleanCode = couponCode.trim();
        Coupon coupon = repository.findByCouponCodeIgnoreCase(cleanCode);
        if (coupon == null) {
            coupon = repository.findByCouponCode(cleanCode);
        }

        if (coupon == null) {
            throw new RuntimeException("Coupon Not Found");
        }

        if (Boolean.FALSE.equals(coupon.getActive())) {
            throw new RuntimeException("Coupon Disabled");
        }

        boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now());
        boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses();

        if (expired || fullyUsed) {
            try {
                repository.delete(coupon);
            } catch (Exception e) {}
            throw new RuntimeException(expired ? "Coupon Expired" : "Coupon Usage Limit Reached");
        }

        return coupon;
    }

    @Transactional
    public Coupon useCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new RuntimeException("Coupon Code is required");
        }

        String cleanCode = couponCode.trim();
        Coupon coupon = repository.findByCouponCodeIgnoreCase(cleanCode);
        if (coupon == null) {
            coupon = repository.findByCouponCode(cleanCode);
        }

        if (coupon == null) {
            throw new RuntimeException("Coupon Not Found");
        }

        int currentUsed = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
        coupon.setUsedCount(currentUsed + 1);

        if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
            try {
                repository.delete(coupon);
            } catch (Exception e) {}
            return coupon;
        }

        return repository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            System.err.println("Failed to delete coupon: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoDeleteInvalidCoupons() {
        try {
            List<Coupon> allCoupons = repository.findAll();
            LocalDate today = LocalDate.now();
            List<Coupon> toDelete = new ArrayList<>();
            for (Coupon coupon : allCoupons) {
                if (coupon == null) continue;
                boolean expired = coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(today);
                boolean fullyUsed = coupon.getUsedCount() != null && coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses();
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