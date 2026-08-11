package com.saipraveen.login_registration.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.service.PricingService;
import com.saipraveen.login_registration.service.CampusBlockService;

@RestController
@RequestMapping("/api/blocks")
@CrossOrigin(origins = "http://localhost:5173")
public class CampusBlockController {

    @Autowired
    private CampusBlockRepository repository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private CampusBlockService campusBlockService;

    @Autowired
    private com.saipraveen.login_registration.service.PrinterConfigService printerConfigService;

    @GetMapping("/all")
    public List<CampusBlock> getAllBlocks() {
        return repository.findAll();
    }

    @GetMapping("/online")
    public List<CampusBlock> getOnlineBlocks(@RequestParam(required = false) String printType) {
        List<CampusBlock> all = repository.findAll();
        String type = (printType != null && !printType.trim().isEmpty()) ? printType : "BW";
        return all.stream()
                .filter(b -> printerConfigService.checkPrinterAvailability(b.getName(), type).isAvailable())
                .toList();
    }

    @PutMapping("/rename/{id}")
    public ResponseEntity<?> renameBlock(@PathVariable Long id, @RequestParam String newName) {
        try {
            CampusBlock updated = campusBlockService.renameBlock(id, newName);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBlock(@PathVariable Long id) {
        try {
            campusBlockService.deleteBlock(id);
            return ResponseEntity.ok("Block deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addBlock(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "KLU") String college
    ) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Block name cannot be empty");
        }
        
        String trimmed = name.trim();
        CampusBlock existing = repository.findByName(trimmed);
        if (existing != null) {
            return ResponseEntity.badRequest().body("Block already exists");
        }

        String col = (college == null || college.trim().isEmpty()) ? "KLU" : college.trim();
        CampusBlock newBlock = new CampusBlock(trimmed, col);
        newBlock.setServerApiKey(java.util.UUID.randomUUID().toString());
        CampusBlock block = repository.save(newBlock);
        
        // Auto-initialize prices for the new block so that we can price orders
        pricingService.updatePrice("BW", 2.0, trimmed);
        pricingService.updatePrice("COLOR", 5.0, trimmed);
        
        return ResponseEntity.ok(block);
    }

    @PostMapping("/generate-key/{id}")
    public ResponseEntity<?> generateServerApiKey(@PathVariable Long id) {
        try {
            CampusBlock block = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Block not found"));
            String newKey = java.util.UUID.randomUUID().toString();
            block.setServerApiKey(newKey);
            repository.save(block);
            return ResponseEntity.ok(block);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
