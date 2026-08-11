package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.Notification;
import com.saipraveen.login_registration.repository.NotificationRepository;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    @Autowired
    private NotificationRepository repository;

    @GetMapping("/user")
    public ResponseEntity<?> getNotificationsForUser(@RequestParam(defaultValue = "KLU") String college) {
        List<Notification> list = repository.findByCollegeIn(Arrays.asList("ALL", college, college.toUpperCase()));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createNotification(@RequestBody Notification notification) {
        if (notification.getCollege() == null || notification.getCollege().trim().isEmpty()) {
            notification.setCollege("ALL");
        }
        return ResponseEntity.ok(repository.save(notification));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteNotification(@RequestParam Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok("Notification deleted");
    }
}
