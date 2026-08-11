package com.saipraveen.login_registration.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.saipraveen.login_registration.service.SseService;

@RestController
@RequestMapping("/api/sse")
@CrossOrigin(originPatterns = "*")
public class SseController {

    @Autowired
    private SseService sseService;

    /**
     * Primary SSE stream endpoint for real-time frontend updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseService.registerClient();
    }

    /**
     * Trigger a manual broadcast (for testing or external webhook integration)
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> triggerBroadcast(
            @RequestParam String event,
            @RequestParam(required = false, defaultValue = "Manual Trigger") String message) {
        sseService.broadcast(event, Map.of("message", message, "timestamp", System.currentTimeMillis()));
        return ResponseEntity.ok(Map.of("success", true, "event", event));
    }
}
