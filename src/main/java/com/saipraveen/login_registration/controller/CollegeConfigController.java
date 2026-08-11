package com.saipraveen.login_registration.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.saipraveen.login_registration.entity.CollegeConfig;
import com.saipraveen.login_registration.repository.CollegeConfigRepository;

@RestController
@RequestMapping("/api/college-config")
@CrossOrigin(origins = "http://localhost:5173")
public class CollegeConfigController {

    @Autowired
    private CollegeConfigRepository collegeConfigRepository;

    @GetMapping
    public List<CollegeConfig> getAllConfigs() {
        return collegeConfigRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<CollegeConfig> saveOrUpdateConfig(@RequestBody CollegeConfig request) {
        if (request.getCollegeName() == null || request.getCollegeName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CollegeConfig existing = collegeConfigRepository.findByCollegeName(request.getCollegeName());
        if (existing != null) {
            existing.setRazorpayKeyId(request.getRazorpayKeyId());
            existing.setRazorpayKeySecret(request.getRazorpayKeySecret());
            return ResponseEntity.ok(collegeConfigRepository.save(existing));
        }

        return ResponseEntity.ok(collegeConfigRepository.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        collegeConfigRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
