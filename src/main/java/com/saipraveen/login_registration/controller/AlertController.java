package com.saipraveen.login_registration.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.service.AlertNotificationService;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAlerts() {
        return ResponseEntity.ok(AlertNotificationService.getPendingAlerts());
    }

    @PostMapping("/ack")
    public ResponseEntity<?> acknowledgeAlert(@RequestParam String id) {
        AlertNotificationService.acknowledgeAlert(id);
        return ResponseEntity.ok("Alert acknowledged");
    }
}
