package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.service.PricingService;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "http://localhost:5173")
public class PricingController {

    @Autowired
    private PricingService service;

    @GetMapping("/all")
    public ResponseEntity<?> getPrices(@RequestParam(required = false) String blockLocation) {
        if (blockLocation != null && !blockLocation.trim().isEmpty()) {
            return ResponseEntity.ok(service.getPricesByBlock(blockLocation));
        }
        return ResponseEntity.ok(service.getPrices());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePrice(
            @RequestParam String printType,
            @RequestParam Double pricePerPage,
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.updatePrice(printType, pricePerPage, blockLocation)
        );
    }

    @GetMapping("/price")
    public ResponseEntity<?> getPrice(
            @RequestParam String printType,
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.getPrice(printType, blockLocation)
        );
    }

    @GetMapping("/rates")
    public ResponseEntity<?> getRates() {
        java.util.Map<String, Double> rates = new java.util.HashMap<>();
        Double bw = service.getPrice("BW", "C Block");
        Double color = service.getPrice("COLOR", "C Block");
        Double duplex = service.getPrice("DUPLEX", "C Block");
        rates.put("bwRate", bw != 0.0 ? bw : 2.0);
        rates.put("colorRate", color != 0.0 ? color : 5.0);
        rates.put("duplexRate", duplex != 0.0 ? duplex : 2.0);
        return ResponseEntity.ok(rates);
    }

    @PostMapping("/rates/update")
    public ResponseEntity<?> updateRates(
            @RequestParam Double bwRate,
            @RequestParam Double colorRate,
            @RequestParam(required = false) Double duplexRate
    ) {
        for (com.saipraveen.login_registration.entity.Pricing p : service.getPrices()) {
            if ("BW".equalsIgnoreCase(p.getPrintType())) {
                service.updatePrice("BW", bwRate, p.getBlockLocation());
            } else if ("COLOR".equalsIgnoreCase(p.getPrintType())) {
                service.updatePrice("COLOR", colorRate, p.getBlockLocation());
            } else if ("DUPLEX".equalsIgnoreCase(p.getPrintType()) && duplexRate != null) {
                service.updatePrice("DUPLEX", duplexRate, p.getBlockLocation());
            }
        }
        return ResponseEntity.ok("Rates updated successfully");
    }
}