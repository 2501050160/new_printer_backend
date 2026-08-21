package com.saipraveen.login_registration.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.repository.CouponRepository;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;

@RestController
@RequestMapping("/api/public/stats")
@CrossOrigin(origins = "*")
public class PublicStatsController {

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    @Autowired(required = false)
    private CouponRepository couponRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Map<String, Object> getLandingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long activePrinters = printerConfigRepository.countByActiveTrue();
        stats.put("activePrinters", activePrinters > 0 ? activePrinters : 27);

        Long pagesPrinted = pdfFileRepository.getTotalPagesPrinted();
        stats.put("pagesPrinted", pagesPrinted != null && pagesPrinted > 0 ? pagesPrinted : 102540);

        Long studentsServed = pdfFileRepository.countDistinctUsersWithCompletedOrders();
        stats.put("studentsServed", studentsServed != null && studentsServed > 0 ? studentsServed : 15420);

        Long totalPaidOrders = pdfFileRepository.countTotalPaidOrders();
        Long completedOrders = pdfFileRepository.getCompletedOrders();
        double successRate = 99.8;
        if (totalPaidOrders != null && totalPaidOrders > 0 && completedOrders != null) {
            successRate = ((double) completedOrders / totalPaidOrders) * 100.0;
            successRate = Math.round(successRate * 10.0) / 10.0;
            if (successRate > 100.0) successRate = 100.0;
        }
        stats.put("successRate", successRate);

        return stats;
    }

    @GetMapping("/coupons")
    public Object getPublicCoupons() {
        try {
            if (couponRepository != null) {
                return couponRepository.findAll();
            }
            if (jdbcTemplate != null) {
                return jdbcTemplate.queryForList("SELECT * FROM coupons");
            }
            return Collections.emptyList();
        } catch (Throwable e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString(), "class", e.getClass().getName());
        }
    }
}
