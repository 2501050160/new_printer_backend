package com.saipraveen.login_registration.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.Popup;
import com.saipraveen.login_registration.repository.PopupRepository;

@RestController
@RequestMapping("/api/popups")
@CrossOrigin(origins = "http://localhost:5173")
public class PopupController {

    @Autowired
    private PopupRepository repository;

    @GetMapping("/active")
    public ResponseEntity<?> getActivePopups(
            @RequestParam(required = false) String page,
            @RequestParam(defaultValue = "KLU") String college) {
        try {
            List<Popup> allActive = repository.findByActiveTrue();
            List<Popup> matched = new ArrayList<>();
            for (Popup p : allActive) {
                String colTarget = p.getCollege();
                if (!"ALL".equalsIgnoreCase(colTarget) && !colTarget.equalsIgnoreCase(college)) {
                    continue;
                }

                if ("ALL".equalsIgnoreCase(p.getTargetPage())) {
                    matched.add(p);
                } else if (page != null && page.equalsIgnoreCase(p.getTargetPage())) {
                    matched.add(p);
                }
            }
            return ResponseEntity.ok(matched);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\nStacktrace:\n" + sw.toString());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Popup>> getAllPopups() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addPopup(@RequestBody Popup popup) {
        if (popup.getTitle() == null || popup.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title cannot be empty");
        }
        if (popup.getMessage() == null || popup.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }
        if (popup.getTargetPage() == null || popup.getTargetPage().trim().isEmpty()) {
            popup.setTargetPage("ALL");
        }
        if (popup.getActive() == null) {
            popup.setActive(true);
        }
        if (popup.getDismissible() == null) {
            popup.setDismissible(true);
        }
        
        Popup saved = repository.save(popup);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam Boolean active) {
        return repository.findById(id).map(pop -> {
            pop.setActive(active);
            repository.save(pop);
            return ResponseEntity.ok("Popup status updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePopup(@RequestParam Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok("Popup deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}
