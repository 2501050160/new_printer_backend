package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.saipraveen.login_registration.service.SettlementService;
import com.saipraveen.login_registration.entity.SettlementRecord;

import java.util.Map;

@RestController
@RequestMapping("/api/settlements")
@CrossOrigin(origins = {"http://localhost:5173", "https://www.cloudprint.website", "https://cloudprint.website", "https://www.saipraveen.site", "https://saipraveen.site"})
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/balance")
    public ResponseEntity<?> getBalanceSummary(
            @RequestParam(required = false, defaultValue = "KLU") String college,
            @RequestParam(required = false, defaultValue = "Admin") String adminUsername,
            @RequestParam(required = false, defaultValue = "SUB_ADMIN") String adminRole
    ) {
        boolean isMainAdmin = "admin".equalsIgnoreCase(adminUsername) || "MAIN_ADMIN".equalsIgnoreCase(adminRole);
        if (isMainAdmin && ("ALL".equalsIgnoreCase(college) || "ALL_COLLEGES".equalsIgnoreCase(college))) {
            return ResponseEntity.ok(settlementService.getAllCollegesBalanceSummary());
        }
        return ResponseEntity.ok(settlementService.getCollegeBalanceSummary(college));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getSettlementHistory(
            @RequestParam(required = false) String college,
            @RequestParam(required = false, defaultValue = "Admin") String adminUsername,
            @RequestParam(required = false, defaultValue = "SUB_ADMIN") String adminRole
    ) {
        return ResponseEntity.ok(settlementService.getSettlementHistory(college, adminUsername, adminRole));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSettlement(
            @RequestParam String adminUsername,
            @RequestBody Map<String, Object> request
    ) {
        try {
            String college = (String) request.get("college");
            Object amtObj = request.get("amount");
            Double amount = amtObj != null ? Double.parseDouble(amtObj.toString()) : 0.0;
            String referenceId = (String) request.get("referenceId");
            String paymentMode = (String) request.get("paymentMode");
            String notes = (String) request.get("notes");

            SettlementRecord record = settlementService.createSettlement(
                    adminUsername,
                    college,
                    amount,
                    referenceId,
                    paymentMode,
                    notes
            );
            return ResponseEntity.ok(record);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(se.getMessage());
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(iae.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create settlement: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSettlement(
            @PathVariable Long id,
            @RequestParam String adminUsername
    ) {
        try {
            settlementService.deleteSettlement(id, adminUsername);
            return ResponseEntity.ok("Settlement record deleted successfully");
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(se.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
