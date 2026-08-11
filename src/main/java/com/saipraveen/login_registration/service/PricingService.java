package com.saipraveen.login_registration.service;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.Pricing;
import com.saipraveen.login_registration.repository.PricingRepository;

@Service
public class PricingService {

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private com.saipraveen.login_registration.repository.CampusBlockRepository campusBlockRepository;

    @PostConstruct
    public void initDefaultPrices() {
        try {
            String[] blocks = {"C Block", "R Block", "L Block"};
            for (String block : blocks) {
                if (campusBlockRepository.findByName(block) == null) {
                    campusBlockRepository.save(new com.saipraveen.login_registration.entity.CampusBlock(block));
                }
                initializeBlockPrice(block, "BW", 2.0);
                initializeBlockPrice(block, "COLOR", 5.0);
                initializeBlockPrice(block, "DUPLEX", 2.0);
            }
        } catch (Exception e) {
            System.err.println("Warning: Default pricing initialization deferred: " + e.getMessage());
        }
    }

    private void initializeBlockPrice(String block, String printType, Double price) {
        List<Pricing> existing = pricingRepository.findAllByPrintTypeAndBlockLocation(printType, block);
        if (existing == null || existing.isEmpty()) {
            Pricing pricing = new Pricing();
            pricing.setBlockLocation(block);
            pricing.setPrintType(printType);
            pricing.setPricePerPage(price);
            pricingRepository.save(pricing);
        }
    }

    public List<Pricing> getPrices() {
        return pricingRepository.findAll();
    }

    public List<Pricing> getPricesByBlock(String blockLocation) {
        return pricingRepository.findByBlockLocation(blockLocation);
    }

    public Pricing updatePrice(String printType, Double pricePerPage, String blockLocation) {
        List<Pricing> list = pricingRepository.findAllByPrintTypeAndBlockLocation(printType, blockLocation);
        Pricing pricing;
        if (list != null && !list.isEmpty()) {
            pricing = list.get(0);
            for (int i = 1; i < list.size(); i++) {
                try { pricingRepository.delete(list.get(i)); } catch (Exception e) {}
            }
        } else {
            pricing = new Pricing();
            pricing.setPrintType(printType);
            pricing.setBlockLocation(blockLocation);
        }
        pricing.setPricePerPage(pricePerPage);
        return pricingRepository.save(pricing);
    }

    public Double getPrice(String printType, String blockLocation) {
        List<Pricing> list = pricingRepository.findAllByPrintTypeAndBlockLocation(printType, blockLocation);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getPricePerPage();
        }
        java.util.List<Pricing> global = pricingRepository.findByPrintType(printType);
        if (global != null && !global.isEmpty()) {
            return global.get(0).getPricePerPage();
        }
        return 0.0;
    }
}