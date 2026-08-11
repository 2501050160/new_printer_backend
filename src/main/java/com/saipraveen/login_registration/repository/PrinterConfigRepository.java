package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.PrinterConfig;

import java.util.List;

public interface PrinterConfigRepository
        extends JpaRepository<PrinterConfig, Long> {

    PrinterConfig findByBlockLocation(
            String blockLocation
    );

    List<PrinterConfig> findAllByBlockLocation(
            String blockLocation
    );

    PrinterConfig findFirstByBlockLocationAndColourSupportedAndActiveTrue(
            String blockLocation, Boolean colourSupported
    );

    PrinterConfig findFirstByBlockLocationAndActiveTrue(
            String blockLocation
    );

    List<PrinterConfig> findByBlockLocationAndColourSupported(
            String blockLocation, Boolean colourSupported
    );

    long countByActiveTrue();
}