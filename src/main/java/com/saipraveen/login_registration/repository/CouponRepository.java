package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.Coupon;

public interface CouponRepository
        extends JpaRepository<Coupon, Long> {

    java.util.List<Coupon> findByCouponCode(
            String couponCode
    );

    java.util.List<Coupon> findByCouponCodeIgnoreCase(
            String couponCode
    );
}