package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.service.PrinterConfigService;

@RestController
@RequestMapping("/api/printer")
public class PrinterConfigController {

    @Autowired
    private PrinterConfigService service;

@PostMapping("/save")
public ResponseEntity<?> savePrinter(
        @RequestBody PrinterConfig printer
) {

    System.out.println("SAVE API HIT");
    System.out.println("BLOCK = " + printer.getBlockLocation());
    System.out.println("PRINTER = " + printer.getPrinterName());

    return ResponseEntity.ok(
            service.savePrinter(printer)
    );
}
    @GetMapping("/all")
    public ResponseEntity<?> getAllPrinters() {

        return ResponseEntity.ok(
                service.getAllPrinters()
        );
    }

    @GetMapping("/byBlock")
    public ResponseEntity<?> getPrinter(
            @RequestParam String blockLocation,
            @RequestParam(required = false) String printType
    ) {
        if (printType != null && !printType.trim().isEmpty()) {
            return ResponseEntity.ok(
                    service.getPrinterByBlockAndType(blockLocation, printType)
            );
        }
        return ResponseEntity.ok(
                service.getPrinterByBlock(blockLocation)
        );
    }

    @GetMapping("/allByBlock")
    public ResponseEntity<?> getAllPrintersByBlock(
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.getAllPrintersByBlock(blockLocation)
        );
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePrinter(
            @RequestParam Long id
    ) {

        service.deletePrinter(id);

        return ResponseEntity.ok("Printer deleted");
    }

    @PostMapping("/toggleBwForColor")
    public ResponseEntity<?> toggleBwForColor(@RequestParam Long id) {
        PrinterConfig updated = service.toggleBwForColor(id);
        if (updated == null) {
            return ResponseEntity.badRequest().body("Printer not found");
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/paper")
    public ResponseEntity<?> getPaperCount(@RequestParam String blockLocation) {
        PrinterConfig printer = service.getPrinterByBlock(blockLocation);
        if (printer == null) {
            return ResponseEntity.ok(0);
        }
        return ResponseEntity.ok(printer.getPaperCount() != null ? printer.getPaperCount() : 0);
    }

    @Autowired
    private com.saipraveen.login_registration.service.AlertNotificationService alertService;

    @PostMapping("/updatePaper")
    public ResponseEntity<?> updatePaperCount(
            @RequestParam String blockLocation,
            @RequestParam Integer paperCount
    ) {
        service.updatePaperCount(blockLocation, paperCount);
        if (paperCount != null && paperCount <= 0) {
            alertService.triggerPrinterAlert(blockLocation, null, "OUT_OF_PAPER", "Paper tray is empty (0 sheets remaining).", null, null);
        } else if (paperCount != null && paperCount <= 15) {
            alertService.triggerPrinterAlert(blockLocation, null, "LOW_PAPER", "Paper count is critically low (" + paperCount + " sheets remaining).", null, null);
        }
        return ResponseEntity.ok("Paper count updated successfully");
    }

    @PostMapping("/report-issue")
    public ResponseEntity<?> reportIssue(
            @RequestBody java.util.Map<String, String> payload
    ) {
        String blockLocation = payload.get("blockLocation");
        String printerName = payload.get("printerName");
        String issueType = payload.get("issueType");
        String details = payload.get("details");
        String orderId = payload.get("orderId");
        String testerPhone = payload.get("testerPhone");

        return ResponseEntity.ok(
                alertService.triggerPrinterAlert(blockLocation, printerName, issueType, details, orderId, testerPhone)
        );
    }

    @GetMapping("/availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam String blockLocation,
            @RequestParam(required = false, defaultValue = "BW") String printType
    ) {
        PrinterConfigService.AvailabilityResult result = service.checkPrinterAvailability(blockLocation, printType);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("available", result.isAvailable());
        response.put("message", result.getMessage());
        return ResponseEntity.ok(response);
    }
}