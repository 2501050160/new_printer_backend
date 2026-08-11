package com.saipraveen.login_registration.controller;

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
    public ResponseEntity<?> createCoupon(

            @RequestBody Coupon coupon

    ) {

        return ResponseEntity.ok(

                service.createCoupon(
                        coupon
                )
        );
    }

    @GetMapping("/all")
    public ResponseEntity<?> getCoupons() {

        return ResponseEntity.ok(

                service.getAllCoupons()
        );
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(

            @RequestParam String couponCode

    ) {

        return ResponseEntity.ok(

                service.validateCoupon(
                        couponCode
                )
        );
    }

    @PostMapping("/use")
public ResponseEntity<?> useCoupon(

        @RequestParam String couponCode

) {

    return ResponseEntity.ok(

            service.useCoupon(
                    couponCode
            )
    );
}

@PostMapping("/delete")
public ResponseEntity<?> deleteCoupon(

        @RequestParam Long id

) {

    service.deleteCoupon(id);

    return ResponseEntity.ok(
            "Coupon Deleted"
    );
}

}