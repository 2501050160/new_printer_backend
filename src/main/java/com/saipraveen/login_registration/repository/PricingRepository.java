package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.Pricing;

public interface PricingRepository
        extends JpaRepository<Pricing, Long> {

    java.util.List<Pricing> findByPrintType(String printType);

    Pricing findByPrintTypeAndBlockLocation(String printType, String blockLocation);

    java.util.List<Pricing> findAllByPrintTypeAndBlockLocation(String printType, String blockLocation);

    java.util.List<Pricing> findByBlockLocation(String blockLocation);
}