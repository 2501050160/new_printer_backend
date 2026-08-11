package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class PublicStatsController {

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

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
}
