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

        String botName = col.isEmpty() ? "Unified Cloud Print Bot" : (col + " Dedicated WhatsApp Bot");
        String jsonContent = String.format(
            "{\n" +
            "  \"targetCollege\": \"%s\",\n" +
            "  \"backendUrl\": \"https://printer-backend-kgzp.onrender.com\",\n" +
            "  \"frontendUrl\": \"https://cloudprint.website\",\n" +
            "  \"botName\": \"%s\"\n" +
            "}\n",
            col,
            botName
        );

        byte[] bytes = jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bot_config.json\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(bytes);
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
        CollegeConfig config = collegeConfigRepository.findByCollegeNameIgnoreCase(col);
        if (config != null) {
            config.setBotLogoutRequested(false);
            collegeConfigRepository.save(config);
        }
        return ResponseEntity.ok(java.util.Map.of("status", "ACKNOWLEDGED"));
    }
}
