package com.saipraveen.login_registration.controller;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.service.QueueService;
import com.saipraveen.login_registration.service.PrinterConfigService;
import com.saipraveen.login_registration.entity.PrinterConfig;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = {"http://localhost:5173", "https://www.cloudprint.website", "https://cloudprint.website", "https://www.saipraveen.site", "https://saipraveen.site"})
public class SystemStatusController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private QueueService queueService;

    @Autowired
    private PrinterConfigService printerService;

    @GetMapping("/db-status")
    public ResponseEntity<?> getDbStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean connected = checkDbConnection();
        status.put("databaseConnected", connected);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus(@RequestParam String blockLocation) {
        Map<String, Object> status = new HashMap<>();
        
        boolean dbConnected = checkDbConnection();
        boolean agentOnline = queueService.isAgentOnline(blockLocation);
        boolean printerConfigured = false;
        boolean maintenance = false;
        boolean active = false;
        boolean paused = false;
        
        Integer paperCount = 500;
        try {
            PrinterConfig config = printerService.getPrinterByBlock(blockLocation);
            if (config != null) {
                active = Boolean.TRUE.equals(config.getActive());
                paused = Boolean.TRUE.equals(config.getPaused());
                maintenance = Boolean.TRUE.equals(config.getMaintenance());
                paperCount = config.getPaperCount() != null ? config.getPaperCount() : 0;
                
                if (active && !paused && config.getPrinterName() != null && !config.getPrinterName().trim().isEmpty()) {
                    printerConfigured = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to look up printer config: " + e.getMessage());
        }

        boolean available = dbConnected && active && !paused && !maintenance && printerConfigured;

        status.put("databaseConnected", dbConnected);
        status.put("agentOnline", agentOnline);
        status.put("printerConfigured", printerConfigured);
        status.put("maintenance", maintenance);
        status.put("active", active);
        status.put("paused", paused);
        status.put("paperCount", paperCount);
        status.put("available", available);
        
        return ResponseEntity.ok(status);
    }

    @Autowired
    private com.saipraveen.login_registration.service.SystemSettingService systemSettingService;

    @GetMapping("/settings")
    public ResponseEntity<?> getPublicSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("referralEnabled", systemSettingService.getSettingBool("referral_enabled", true));
        settings.put("referrerAmount", systemSettingService.getSettingDouble("referral_referrer_amount", 10.0));
        settings.put("refereeAmount", systemSettingService.getSettingDouble("referral_referee_amount", 5.0));
        settings.put("popupEnabled", systemSettingService.getSettingBool("referral_popup_enabled", true));
        settings.put("popupMessage", systemSettingService.getSetting("referral_popup_message", ""));
        settings.put("adEnabled", systemSettingService.getSettingBool("ad_enabled", true));
        settings.put("adText", systemSettingService.getSetting("ad_text", ""));
        settings.put("generalPopupEnabled", systemSettingService.getSettingBool("general_popup_enabled", false));
        settings.put("generalPopupMessage", systemSettingService.getSetting("general_popup_message", ""));
        settings.put("suspendedColleges", systemSettingService.getSetting("suspended_colleges", ""));
        settings.put("displayAdPhotoEnabled", systemSettingService.getSettingBool("display_ad_photo_enabled", true));
        settings.put("testerModeEnabled", systemSettingService.getSettingBool("tester_mode_enabled", false));
        settings.put("testerUsernames", systemSettingService.getSetting("tester_usernames", ""));
        return ResponseEntity.ok(settings);
    }

    @Autowired
    private com.saipraveen.login_registration.repository.UserRepository userRepository;

    @Autowired
    private com.saipraveen.login_registration.repository.PdfFileRepository pdfFileRepository;

    @GetMapping("/public-stats")
    public ResponseEntity<?> getPublicStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalUsers = userRepository.count();
            long totalDocuments = pdfFileRepository.count();
            stats.put("studentCount", totalUsers);
            stats.put("documentCount", totalDocuments);
            stats.put("campusLocations", 8);
            stats.put("successRate", 99.9);
        } catch (Exception e) {
            stats.put("studentCount", 12500);
            stats.put("documentCount", 15000);
            stats.put("campusLocations", 8);
            stats.put("successRate", 99.9);
        }
        return ResponseEntity.ok(stats);
    }

    private boolean checkDbConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
