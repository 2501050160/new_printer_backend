package com.saipraveen.login_registration.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.saipraveen.login_registration.entity.CollegeConfig;
import com.saipraveen.login_registration.repository.CollegeConfigRepository;

@RestController
@RequestMapping("/api/college-config")
@CrossOrigin(originPatterns = "*")
public class CollegeConfigController {

    @Autowired
    private CollegeConfigRepository collegeConfigRepository;

    @GetMapping
    public List<CollegeConfig> getAllConfigs() {
        return collegeConfigRepository.findAll();
    }

    @PostMapping({"", "/update"})
    public ResponseEntity<CollegeConfig> saveOrUpdateConfig(@RequestBody CollegeConfig request) {
        String cName = request.getCollegeName();
        if (cName == null || cName.trim().isEmpty()) {
            cName = request.getCollege();
        }
        if (cName == null || cName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        cName = cName.trim();
        request.setCollegeName(cName);

        CollegeConfig existing = collegeConfigRepository.findByCollegeName(cName);
        if (existing != null) {
            if (request.getRazorpayKeyId() != null) {
                existing.setRazorpayKeyId(request.getRazorpayKeyId());
            }
            if (request.getRazorpayKeySecret() != null) {
                existing.setRazorpayKeySecret(request.getRazorpayKeySecret());
            }
            if (request.getWhatsappBotPhone() != null) {
                existing.setWhatsappBotPhone(request.getWhatsappBotPhone());
            }
            if (request.getDedicatedBotEnabled() != null) {
                existing.setDedicatedBotEnabled(request.getDedicatedBotEnabled());
            }
            if (request.getWhatsappBotApiKey() != null) {
                existing.setWhatsappBotApiKey(request.getWhatsappBotApiKey());
            }
            return ResponseEntity.ok(collegeConfigRepository.save(existing));
        }

        return ResponseEntity.ok(collegeConfigRepository.save(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        collegeConfigRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download-bot-config/{college}")
    public ResponseEntity<byte[]> downloadBotConfig(@PathVariable String college) {
        String col = (college == null || college.trim().isEmpty() || college.equalsIgnoreCase("unified") || college.equalsIgnoreCase("all"))
            ? ""
            : college.trim();

        CollegeConfig config = col.isEmpty() ? null : collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        String botKey = (config != null && config.getWhatsappBotApiKey() != null) ? config.getWhatsappBotApiKey() : "";
        String botName = col.isEmpty() ? "Unified Cloud Print Bot" : (col + " Dedicated WhatsApp Bot");
        String jsonContent = String.format(
            "{\n" +
            "  \"targetCollege\": \"%s\",\n" +
            "  \"botApiKey\": \"%s\",\n" +
            "  \"backendUrl\": \"https://printer-backend-kgzp.onrender.com\",\n" +
            "  \"frontendUrl\": \"https://cloudprint.website\",\n" +
            "  \"botName\": \"%s\"\n" +
            "}\n",
            col,
            botKey,
            botName
        );

        byte[] bytes = jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bot_config.json\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(bytes);
    }

    @PostMapping("/generate-bot-key")
    public ResponseEntity<?> generateBotKey(@RequestParam String college) {
        String col = (college == null) ? "" : college.trim();
        if (col.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "College name is required"));
        }
        CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        if (config == null) {
            config = new CollegeConfig();
            config.setCollegeName(col);
            config.setRazorpayKeyId("");
            config.setRazorpayKeySecret("");
        }
        String cleanCol = col.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String generatedKey = "WA_KEY_" + cleanCol + "_" + randomSuffix;
        config.setWhatsappBotApiKey(generatedKey);
        config.setDedicatedBotEnabled(true);
        config.setBotLogoutRequested(true);
        collegeConfigRepository.save(config);
        return ResponseEntity.ok(java.util.Map.of(
            "college", col,
            "botApiKey", generatedKey,
            "status", "SUCCESS",
            "message", "Dedicated WhatsApp Bot API key generated successfully"
        ));
    }

    @GetMapping("/verify-bot-key")
    public ResponseEntity<?> verifyBotKey(@RequestParam String key) {
        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("valid", false, "error", "Missing key"));
        }
        CollegeConfig config = collegeConfigRepository.findByWhatsappBotApiKey(key.trim());
        if (config != null) {
            return ResponseEntity.ok(java.util.Map.of(
                "valid", true,
                "college", config.getCollegeName(),
                "dedicatedBotEnabled", config.getDedicatedBotEnabled(),
                "whatsappBotPhone", config.getWhatsappBotPhone() != null ? config.getWhatsappBotPhone() : ""
            ));
        }
        return ResponseEntity.ok(java.util.Map.of("valid", false, "error", "Invalid or unregistered bot API key"));
    }

    @PostMapping("/bot-logout")
    public ResponseEntity<?> requestBotLogout(@RequestParam String college) {
        String col = (college == null) ? "" : college.trim();
        CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        if (config != null) {
            config.setBotLogoutRequested(true);
            config.setDedicatedBotEnabled(false);
            collegeConfigRepository.save(config);
            return ResponseEntity.ok(java.util.Map.of("message", "Bot logout requested for " + col, "status", "SUCCESS"));
        } else {
            CollegeConfig newConfig = new CollegeConfig();
            newConfig.setCollegeName(col);
            newConfig.setRazorpayKeyId("");
            newConfig.setRazorpayKeySecret("");
            newConfig.setBotLogoutRequested(true);
            newConfig.setDedicatedBotEnabled(false);
            collegeConfigRepository.save(newConfig);
            return ResponseEntity.ok(java.util.Map.of("message", "Bot logout requested for " + col, "status", "SUCCESS"));
        }
    }

    @GetMapping("/bot-status")
    public ResponseEntity<?> getBotStatus(@RequestParam(required = false) String college) {
        String col = (college == null) ? "" : college.trim();
        if (col.isEmpty() || "ALL".equalsIgnoreCase(col) || "UNIFIED".equalsIgnoreCase(col)) {
            java.util.List<CollegeConfig> allConfigs = collegeConfigRepository.findAll();
            for (CollegeConfig c : allConfigs) {
                if (Boolean.TRUE.equals(c.getBotLogoutRequested())) {
                    return ResponseEntity.ok(java.util.Map.of(
                        "college", c.getCollegeName() != null ? c.getCollegeName() : "",
                        "logoutRequested", true,
                        "dedicatedBotEnabled", Boolean.TRUE.equals(c.getDedicatedBotEnabled()),
                        "whatsappBotPhone", c.getWhatsappBotPhone() != null ? c.getWhatsappBotPhone() : ""
                    ));
                }
            }
            return ResponseEntity.ok(java.util.Map.of(
                "college", "UNIFIED",
                "logoutRequested", false,
                "dedicatedBotEnabled", false,
                "whatsappBotPhone", ""
            ));
        }
        CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        boolean logoutRequested = config != null && Boolean.TRUE.equals(config.getBotLogoutRequested());
        return ResponseEntity.ok(java.util.Map.of(
            "college", col,
            "logoutRequested", logoutRequested,
            "dedicatedBotEnabled", config != null && Boolean.TRUE.equals(config.getDedicatedBotEnabled()),
            "whatsappBotPhone", (config != null && config.getWhatsappBotPhone() != null) ? config.getWhatsappBotPhone() : ""
        ));
    }

    @PostMapping("/bot-ack-logout")
    public ResponseEntity<?> ackBotLogout(@RequestParam String college) {
        String col = (college == null) ? "" : college.trim();
        if (col.isEmpty() || "ALL".equalsIgnoreCase(col) || "UNIFIED".equalsIgnoreCase(col)) {
            java.util.List<CollegeConfig> allConfigs = collegeConfigRepository.findAll();
            for (CollegeConfig c : allConfigs) {
                if (Boolean.TRUE.equals(c.getBotLogoutRequested())) {
                    c.setBotLogoutRequested(false);
                    collegeConfigRepository.save(c);
                }
            }
            return ResponseEntity.ok(java.util.Map.of("status", "ACKNOWLEDGED"));
        }
        CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        if (config != null) {
            config.setBotLogoutRequested(false);
            collegeConfigRepository.save(config);
        }
        return ResponseEntity.ok(java.util.Map.of("status", "ACKNOWLEDGED"));
    }
}
