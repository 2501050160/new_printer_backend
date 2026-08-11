package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.service.QueueService;
import com.saipraveen.login_registration.repository.CampusBlockRepository;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @Autowired
    private CampusBlockRepository campusBlockRepository;

    private CampusBlock authenticateAgent(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return campusBlockRepository.findByServerApiKey(token);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending(
            @RequestParam String blockLocation
    ) {

        return ResponseEntity.ok(
                queueService.getQueueByBlock(blockLocation)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive(
            @RequestParam String blockLocation
    ) {

        return ResponseEntity.ok(
                queueService.getActiveQueueByBlock(blockLocation)
        );
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNext(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            CampusBlock block = authenticateAgent(authHeader);
            if (block == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing API Key");
            }
            
            PdfFile next = queueService.getNextForAgent(block.getName());

            if (next == null) {
                return ResponseEntity.ok(null);
            }

            return ResponseEntity.ok(next);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\nStacktrace:\n" + sw.toString());
        }
    }

    @PostMapping("/startPrinting")
    public ResponseEntity<?> startPrinting(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String orderId
    ) {
        CampusBlock block = authenticateAgent(authHeader);
        if (block == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing API Key");
        }

        return ResponseEntity.ok(
                queueService.startPrinting(orderId)
        );
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String orderId
    ) {
        CampusBlock block = authenticateAgent(authHeader);
        if (block == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing API Key");
        }

        return ResponseEntity.ok(
                queueService.completeOrder(orderId)
        );
    }

    @PostMapping("/proceed")
    public ResponseEntity<?> proceed(
            @RequestParam String orderId
    ) {

        return ResponseEntity.ok(
                queueService.proceedOrder(orderId)
        );
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(
            @RequestParam String orderId,
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                queueService.cancelOrder(orderId, userId)
        );
    }
}
