package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.Admin;
import com.saipraveen.login_registration.entity.ManagerLog;
import com.saipraveen.login_registration.repository.ManagerLogRepository;
import com.saipraveen.login_registration.service.AdminService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private AdminService service;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Admin admin
    ) {

        Admin loggedAdmin =
                service.login(
                        admin.getUsername(),
                        admin.getPassword()
                );

        if (loggedAdmin != null) {

            return ResponseEntity.ok(
                    loggedAdmin
            );
        }

        return ResponseEntity
                .badRequest()
                .body("Invalid Admin Credentials");
    }

    @Autowired
    private com.saipraveen.login_registration.service.UserService userService;

    @Autowired
    private com.saipraveen.login_registration.service.PdfFileService pdfFileService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.saipraveen.login_registration.service.SystemSettingService systemSettingService;

    @org.springframework.web.bind.annotation.GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/users/toggle-block")
    public ResponseEntity<?> toggleBlockUser(@org.springframework.web.bind.annotation.RequestParam Long id) {
        return ResponseEntity.ok(userService.toggleBlockUser(id));
    }

    @PostMapping("/users/wallet/add")
    public ResponseEntity<?> addWalletBalance(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam Double amount
    ) {
        return ResponseEntity.ok(userService.creditWallet(id, amount));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/delete")
    public ResponseEntity<?> deleteUser(@org.springframework.web.bind.annotation.RequestParam Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/users/update-college")
    public ResponseEntity<?> updateUserCollege(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam String college,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String adminUsername
    ) {
        if (adminUsername != null && !"admin".equalsIgnoreCase(adminUsername.trim())) {
            return ResponseEntity.badRequest().body("Only the main admin has permission to change user colleges!");
        }
        return ResponseEntity.ok(userService.updateUserCollegeById(id, college));
    }

    @PostMapping("/reset-stats")
    public ResponseEntity<?> resetStats(
            @org.springframework.web.bind.annotation.RequestParam String adminUsername,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String scope,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String targetName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String blockLocation
    ) {
        if (!"admin".equalsIgnoreCase(adminUsername)) {
            return ResponseEntity.badRequest().body("Only the main admin can reset statistics and database records!");
        }
        if (scope != null && !scope.trim().isEmpty()) {
            pdfFileService.resetStatsByScope(scope, targetName);
            return ResponseEntity.ok("Statistics reset successfully for scope: " + scope + (targetName != null ? " (" + targetName + ")" : ""));
        } else if (blockLocation != null && !blockLocation.trim().isEmpty() && !"ALL".equalsIgnoreCase(blockLocation.trim())) {
            pdfFileService.resetStatsByBlock(blockLocation);
            return ResponseEntity.ok("Statistics for block '" + blockLocation + "' reset successfully");
        } else {
            pdfFileService.resetAllStats();
            return ResponseEntity.ok("All statistics reset successfully");
        }
    }

    @PostMapping("/orders/delete-bulk")
    public ResponseEntity<?> deleteOrdersBulk(
            @org.springframework.web.bind.annotation.RequestParam String adminUsername,
            @RequestBody java.util.List<String> orderIds
    ) {
        if (!"admin".equalsIgnoreCase(adminUsername)) {
            return ResponseEntity.badRequest().body("Only the main admin can delete orders from the database!");
        }
        if (orderIds == null || orderIds.isEmpty()) {
            return ResponseEntity.badRequest().body("No order IDs provided");
        }
        pdfFileService.deleteOrdersByOrderIds(orderIds);
        return ResponseEntity.ok("Deleted " + orderIds.size() + " orders successfully");
    }

    @PostMapping("/sql")
    public ResponseEntity<?> executeSql(@RequestBody java.util.Map<String, String> request) {
        String sql = request.get("query");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query cannot be empty");
        }
        
        String trimmed = sql.trim().toLowerCase();
        try {
            if (trimmed.startsWith("select") || trimmed.startsWith("show") || trimmed.startsWith("describe")) {
                java.util.List<java.util.Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                return ResponseEntity.ok(result);
            } else {
                int rowsAffected = jdbcTemplate.update(sql);
                java.util.Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", true);
                res.put("rowsAffected", rowsAffected);
                res.put("message", "Query executed successfully. Rows affected: " + rowsAffected);
                return ResponseEntity.ok(res);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("SQL Error: " + e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("referralEnabled", systemSettingService.getSettingBool("referral_enabled", true));
        settings.put("referrerAmount", systemSettingService.getSettingDouble("referral_referrer_amount", 10.0));
        settings.put("refereeAmount", systemSettingService.getSettingDouble("referral_referee_amount", 5.0));
        settings.put("popupEnabled", systemSettingService.getSettingBool("referral_popup_enabled", true));
        settings.put("popupMessage", systemSettingService.getSetting("referral_popup_message", ""));
        settings.put("adEnabled", systemSettingService.getSettingBool("ad_enabled", true));
        settings.put("adText", systemSettingService.getSetting("ad_text", ""));
        settings.put("generalPopupEnabled", systemSettingService.getSettingBool("general_popup_enabled", false));
        settings.put("generalPopupMessage", systemSettingService.getSetting("general_popup_message", ""));
        settings.put("thesisDiscountPages", systemSettingService.getSettingDouble("thesis_discount_pages", 50.0));
        settings.put("thesisDiscountPercent", systemSettingService.getSettingDouble("thesis_discount_percent", 15.0));
        settings.put("offpeakDiscountPercent", systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0));
        settings.put("offpeakStartHour", systemSettingService.getSettingDouble("offpeak_start_hour", 21.0));
        settings.put("offpeakEndHour", systemSettingService.getSettingDouble("offpeak_end_hour", 7.0));
        settings.put("offpeakMorningStart", systemSettingService.getSettingDouble("offpeak_morning_start", 7.0));
        settings.put("offpeakMorningEnd", systemSettingService.getSettingDouble("offpeak_morning_end", 9.0));
        settings.put("suspendedColleges", systemSettingService.getSetting("suspended_colleges", ""));
        settings.put("cancelWindowEnabled", systemSettingService.getSettingBool("cancel_window_enabled", true));
        settings.put("displayAdPhotoEnabled", systemSettingService.getSettingBool("display_ad_photo_enabled", true));
        settings.put("testerModeEnabled", systemSettingService.getSettingBool("tester_mode_enabled", false));
        settings.put("testerUsernames", systemSettingService.getSetting("tester_usernames", ""));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/update")
    public ResponseEntity<?> updateSettings(@RequestBody java.util.Map<String, Object> request) {
        if (request.containsKey("referralEnabled")) {
            systemSettingService.setSetting("referral_enabled", String.valueOf(request.get("referralEnabled")));
        }
        if (request.containsKey("referrerAmount")) {
            systemSettingService.setSetting("referral_referrer_amount", String.valueOf(request.get("referrerAmount")));
        }
        if (request.containsKey("refereeAmount")) {
            systemSettingService.setSetting("referral_referee_amount", String.valueOf(request.get("refereeAmount")));
        }
        if (request.containsKey("popupEnabled")) {
            systemSettingService.setSetting("referral_popup_enabled", String.valueOf(request.get("popupEnabled")));
        }
        if (request.containsKey("popupMessage")) {
            systemSettingService.setSetting("referral_popup_message", String.valueOf(request.get("popupMessage")));
        }
        if (request.containsKey("adEnabled")) {
            systemSettingService.setSetting("ad_enabled", String.valueOf(request.get("adEnabled")));
        }
        if (request.containsKey("adText")) {
            systemSettingService.setSetting("ad_text", String.valueOf(request.get("adText")));
        }
        if (request.containsKey("generalPopupEnabled")) {
            systemSettingService.setSetting("general_popup_enabled", String.valueOf(request.get("generalPopupEnabled")));
        }
        if (request.containsKey("generalPopupMessage")) {
            systemSettingService.setSetting("general_popup_message", String.valueOf(request.get("generalPopupMessage")));
        }
        if (request.containsKey("thesisDiscountPages")) {
            systemSettingService.setSetting("thesis_discount_pages", String.valueOf(request.get("thesisDiscountPages")));
        }
        if (request.containsKey("thesisDiscountPercent")) {
            systemSettingService.setSetting("thesis_discount_percent", String.valueOf(request.get("thesisDiscountPercent")));
        }
        if (request.containsKey("offpeakDiscountPercent")) {
            systemSettingService.setSetting("offpeak_discount_percent", String.valueOf(request.get("offpeakDiscountPercent")));
        }
        if (request.containsKey("offpeakStartHour")) {
            systemSettingService.setSetting("offpeak_start_hour", String.valueOf(request.get("offpeakStartHour")));
        }
        if (request.containsKey("offpeakEndHour")) {
            systemSettingService.setSetting("offpeak_end_hour", String.valueOf(request.get("offpeakEndHour")));
        }
        if (request.containsKey("offpeakMorningStart")) {
            systemSettingService.setSetting("offpeak_morning_start", String.valueOf(request.get("offpeakMorningStart")));
        }
        if (request.containsKey("offpeakMorningEnd")) {
            systemSettingService.setSetting("offpeak_morning_end", String.valueOf(request.get("offpeakMorningEnd")));
        }
        if (request.containsKey("suspendedColleges")) {
            systemSettingService.setSetting("suspended_colleges", String.valueOf(request.get("suspendedColleges")));
        }
        if (request.containsKey("cancelWindowEnabled")) {
            systemSettingService.setSetting("cancel_window_enabled", String.valueOf(request.get("cancelWindowEnabled")));
        }
        if (request.containsKey("displayAdPhotoEnabled")) {
            systemSettingService.setSetting("display_ad_photo_enabled", String.valueOf(request.get("displayAdPhotoEnabled")));
        }
        if (request.containsKey("testerModeEnabled")) {
            systemSettingService.setSetting("tester_mode_enabled", String.valueOf(request.get("testerModeEnabled")));
        }
        if (request.containsKey("testerUsernames")) {
            systemSettingService.setSetting("tester_usernames", String.valueOf(request.get("testerUsernames")));
        }
        return ResponseEntity.ok("Settings updated successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/settings/offpeak")
    public ResponseEntity<?> getCollegeOffpeakSettings(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college) {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("offpeakEnabled", systemSettingService.getSettingBool("offpeak_enabled_" + college, systemSettingService.getSettingBool("offpeak_enabled", true)));
        settings.put("offpeakDiscountPercent", systemSettingService.getSettingDouble("offpeak_discount_percent_" + college, systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0)));
        settings.put("offpeakStartHour", systemSettingService.getSettingDouble("offpeak_start_hour_" + college, systemSettingService.getSettingDouble("offpeak_start_hour", 21.0)));
        settings.put("offpeakEndHour", systemSettingService.getSettingDouble("offpeak_end_hour_" + college, systemSettingService.getSettingDouble("offpeak_end_hour", 7.0)));
        settings.put("offpeakMorningStart", systemSettingService.getSettingDouble("offpeak_morning_start_" + college, systemSettingService.getSettingDouble("offpeak_morning_start", 7.0)));
        settings.put("offpeakMorningEnd", systemSettingService.getSettingDouble("offpeak_morning_end_" + college, systemSettingService.getSettingDouble("offpeak_morning_end", 9.0)));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/offpeak/update")
    public ResponseEntity<?> updateCollegeOffpeakSettings(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college,
            @RequestBody java.util.Map<String, Object> request) {
        if (request.containsKey("offpeakEnabled")) {
            systemSettingService.setSetting("offpeak_enabled_" + college, String.valueOf(request.get("offpeakEnabled")));
        }
        if (request.containsKey("offpeakDiscountPercent")) {
            systemSettingService.setSetting("offpeak_discount_percent_" + college, String.valueOf(request.get("offpeakDiscountPercent")));
        }
        if (request.containsKey("offpeakStartHour")) {
            systemSettingService.setSetting("offpeak_start_hour_" + college, String.valueOf(request.get("offpeakStartHour")));
        }
        if (request.containsKey("offpeakEndHour")) {
            systemSettingService.setSetting("offpeak_end_hour_" + college, String.valueOf(request.get("offpeakEndHour")));
        }
        if (request.containsKey("offpeakMorningStart")) {
            systemSettingService.setSetting("offpeak_morning_start_" + college, String.valueOf(request.get("offpeakMorningStart")));
        }
        if (request.containsKey("offpeakMorningEnd")) {
            systemSettingService.setSetting("offpeak_morning_end_" + college, String.valueOf(request.get("offpeakMorningEnd")));
        }
        return ResponseEntity.ok("College off-peak settings updated successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/settings/thesis")
    public ResponseEntity<?> getCollegeThesisSettings(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college) {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("thesisEnabled", systemSettingService.getSettingBool("thesis_enabled_" + college, systemSettingService.getSettingBool("thesis_enabled", true)));
        settings.put("thesisDiscountPages", systemSettingService.getSettingDouble("thesis_discount_pages_" + college, systemSettingService.getSettingDouble("thesis_discount_pages", 500.0)));
        settings.put("thesisDiscountPercent", systemSettingService.getSettingDouble("thesis_discount_percent_" + college, systemSettingService.getSettingDouble("thesis_discount_percent", 15.0)));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/thesis/update")
    public ResponseEntity<?> updateCollegeThesisSettings(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college,
            @RequestBody java.util.Map<String, Object> request) {
        if (request.containsKey("thesisEnabled")) {
            systemSettingService.setSetting("thesis_enabled_" + college, String.valueOf(request.get("thesisEnabled")));
            systemSettingService.setSetting("thesis_enabled", String.valueOf(request.get("thesisEnabled")));
        }
        if (request.containsKey("thesisDiscountPages")) {
            systemSettingService.setSetting("thesis_discount_pages_" + college, String.valueOf(request.get("thesisDiscountPages")));
            systemSettingService.setSetting("thesis_discount_pages", String.valueOf(request.get("thesisDiscountPages")));
        }
        if (request.containsKey("thesisDiscountPercent")) {
            systemSettingService.setSetting("thesis_discount_percent_" + college, String.valueOf(request.get("thesisDiscountPercent")));
            systemSettingService.setSetting("thesis_discount_percent", String.valueOf(request.get("thesisDiscountPercent")));
        }
        return ResponseEntity.ok("Thesis & Bulk Print settings updated for " + college);
    }

    @org.springframework.web.bind.annotation.GetMapping("/printers/status")
    public ResponseEntity<?> getPrintersStatus() {
        return ResponseEntity.ok(pdfFileService.getPrinterLiveStatusList());
    }

    @org.springframework.web.bind.annotation.GetMapping("/subadmins")
    public ResponseEntity<?> getAllSubAdmins() {
        return ResponseEntity.ok(service.getAllSubAdmins());
    }

    @PostMapping("/subadmins/create")
    public ResponseEntity<?> createSubAdmin(@RequestBody Admin subAdmin) {
        try {
            return ResponseEntity.ok(service.createSubAdmin(subAdmin));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/subadmins/delete")
    public ResponseEntity<?> deleteSubAdmin(@org.springframework.web.bind.annotation.RequestParam Long id) {
        service.deleteSubAdmin(id);
        return ResponseEntity.ok("Sub-admin deleted successfully");
    }

    @PostMapping("/verify-secret")
    public ResponseEntity<?> verifySecret(@org.springframework.web.bind.annotation.RequestParam Long adminId, @org.springframework.web.bind.annotation.RequestParam String secret) {
        boolean isValid = service.verifyManagerSecret(adminId, secret);
        if (isValid) {
            return ResponseEntity.ok(java.util.Collections.singletonMap("success", true));
        }
        return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("success", false));
    }

    @Autowired
    private ManagerLogRepository managerLogRepository;

    @PostMapping("/logs/create")
    public ResponseEntity<?> createManagerLog(@RequestBody(required = false) ManagerLog log) {
        try {
            if (log == null) {
                log = new ManagerLog();
            }
            if (log.getManagerName() == null || log.getManagerName().isEmpty()) {
                log.setManagerName("Admin");
            }
            if (log.getCollege() == null || log.getCollege().isEmpty()) {
                log.setCollege("KLU");
            }
            if (log.getTimestamp() == null) {
                log.setTimestamp(java.time.LocalDateTime.now());
            }
            managerLogRepository.save(log);
            return ResponseEntity.ok("Log created successfully");
        } catch (Exception e) {
            return ResponseEntity.ok("Log created successfully");
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/logs/all")
    public ResponseEntity<?> getManagerLogs(@org.springframework.web.bind.annotation.RequestParam(required = false) String college) {
        try {
            if (college != null && !college.isEmpty() && !college.equals("ALL")) {
                return ResponseEntity.ok(managerLogRepository.findByCollegeOrderByTimestampDesc(college));
            }
            return ResponseEntity.ok(managerLogRepository.findAllByOrderByTimestampDesc());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/export-sql")
    public ResponseEntity<String> exportSql() {
        try {
            java.util.List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class
            );

            StringBuilder sqlDump = new StringBuilder();
            sqlDump.append("-- Cloud Print System Database Backup SQL\n");
            sqlDump.append("-- Generated on: ").append(java.time.LocalDateTime.now()).append("\n\n");
            sqlDump.append("SET session_replication_role = 'replica';\n\n");

            for (String tableName : tables) {
                sqlDump.append("-- Dumping data for table: ").append(tableName).append("\n");
                sqlDump.append("TRUNCATE TABLE ").append(tableName).append(" CASCADE;\n");

                String selectQuery = "SELECT * FROM " + tableName;
                jdbcTemplate.query(selectQuery, (java.sql.ResultSet rs) -> {
                    java.sql.ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    StringBuilder columns = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) columns.append(", ");
                        columns.append(metaData.getColumnName(i));
                    }

                    while (rs.next()) {
                        StringBuilder values = new StringBuilder();
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) values.append(", ");

                            String colName = metaData.getColumnName(i);
                            Object val = rs.getObject(i);

                            if (val == null) {
                                values.append("NULL");
                            } else if (colName.equalsIgnoreCase("pdf_data")) {
                                values.append("NULL");
                            } else {
                                if (val instanceof String || val instanceof java.sql.Date || val instanceof java.sql.Timestamp || val instanceof java.time.LocalDateTime || val instanceof java.time.LocalDate) {
                                    String strVal = val.toString().replace("'", "''");
                                    values.append("'").append(strVal).append("'");
                                } else if (val instanceof Boolean) {
                                    values.append(val);
                                } else {
                                    values.append(val.toString());
                                }
                            }
                        }
                        sqlDump.append("INSERT INTO ").append(tableName)
                               .append(" (").append(columns).append(") VALUES (")
                               .append(values).append(");\n");
                    }
                    return null;
                });
                sqlDump.append("\n");
            }

            sqlDump.append("SET session_replication_role = 'origin';\n");

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup.sql")
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(sqlDump.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("-- Failed to export database: " + e.getMessage());
        }
    }
}