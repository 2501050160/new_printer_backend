package com.saipraveen.login_registration.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.repository.CouponRepository;
import com.saipraveen.login_registration.repository.RewardRepository;

@RestController
@CrossOrigin(origins = "*")
public class DiagnosticController {

    @Autowired(required = false)
    private CouponRepository couponRepository;

    @Autowired(required = false)
    private RewardRepository rewardRepository;

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping("/api/diagnostic")
    public Map<String, Object> diagnose() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Check DataSource
        try {
            if (dataSource != null) {
                result.put("dataSource", dataSource.getClass().getName());
                try (var conn = dataSource.getConnection()) {
                    result.put("dbConnected", true);
                    result.put("dbUrl", conn.getMetaData().getURL());
                }
            } else {
                result.put("dataSource", "NULL");
            }
        } catch (Exception e) {
            result.put("dataSourceError", e.getClass().getName() + ": " + e.getMessage());
        }

        // Check RewardRepository (known working)
        try {
            if (rewardRepository != null) {
                int rewardCount = (int) rewardRepository.count();
                result.put("rewardRepoOk", true);
                result.put("rewardCount", rewardCount);
            } else {
                result.put("rewardRepo", "NULL - not injected");
            }
        } catch (Exception e) {
            result.put("rewardRepoError", e.getClass().getName() + ": " + e.getMessage());
        }

        // Check CouponRepository (failing)
        try {
            if (couponRepository != null) {
                result.put("couponRepoInjected", true);
                result.put("couponRepoClass", couponRepository.getClass().getName());
                long count = couponRepository.count();
                result.put("couponCountOk", true);
                result.put("couponCount", count);
            } else {
                result.put("couponRepo", "NULL - not injected!");
            }
        } catch (Exception e) {
            result.put("couponRepoError", e.getClass().getName() + ": " + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                result.put("couponRepoCause", cause.getClass().getName() + ": " + cause.getMessage());
                Throwable root = cause.getCause();
                if (root != null) {
                    result.put("couponRepoRootCause", root.getClass().getName() + ": " + root.getMessage());
                }
            }
        }

        // Try raw JDBC on coupons table
        try {
            if (dataSource != null) {
                try (var conn = dataSource.getConnection();
                     var stmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT count(*) FROM coupons")) {
                    if (rs.next()) {
                        result.put("rawJdbcCouponCount", rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            result.put("rawJdbcError", e.getClass().getName() + ": " + e.getMessage());
        }

        return result;
    }
}
