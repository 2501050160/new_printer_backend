package com.saipraveen.login_registration.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(originPatterns = "*")
public class HealthCheckController {

    @GetMapping({"/", "/health", "/api/health"})
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("service", "Cloud Print Backend");
        map.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(map);
    }
}
