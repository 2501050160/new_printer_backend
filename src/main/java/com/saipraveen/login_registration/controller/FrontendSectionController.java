package com.saipraveen.login_registration.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.FrontendSection;
import com.saipraveen.login_registration.repository.FrontendSectionRepository;

@RestController
@RequestMapping("/api/sections")
@CrossOrigin(origins = "http://localhost:5173")
public class FrontendSectionController {

    @Autowired
    private FrontendSectionRepository repository;

    @GetMapping("/active")
    public ResponseEntity<List<FrontendSection>> getActiveSections(@RequestParam(defaultValue = "KLU") String college) {
        List<FrontendSection> allActive = repository.findByActiveTrueOrderByDisplayOrderAsc();
        List<FrontendSection> filtered = allActive.stream()
            .filter(s -> "ALL".equalsIgnoreCase(s.getCollege()) || s.getCollege().equalsIgnoreCase(college))
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/all")
    public ResponseEntity<List<FrontendSection>> getAllSections() {
        return ResponseEntity.ok(repository.findAllByOrderByDisplayOrderAsc());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addSection(@RequestBody FrontendSection section) {
        if (section.getTitle() == null || section.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title cannot be empty");
        }
        if (section.getContent() == null || section.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Content cannot be empty");
        }
        if (section.getSectionType() == null || section.getSectionType().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Section type cannot be empty");
        }
        if (section.getActive() == null) {
            section.setActive(true);
        }
        if (section.getDisplayOrder() == null) {
            section.setDisplayOrder(0);
        }
        
        FrontendSection saved = repository.save(section);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam Boolean active) {
        return repository.findById(id).map(sec -> {
            sec.setActive(active);
            repository.save(sec);
            return ResponseEntity.ok("Section status updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteSection(@RequestParam Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok("Section deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}
