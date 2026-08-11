package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.Coupon;

public interface CouponRepository
        extends JpaRepository<Coupon, Long> {

    Coupon findByCouponCode(
            String couponCode
    );

    Coupon findByCouponCodeIgnoreCase(
            String couponCode
    );
}