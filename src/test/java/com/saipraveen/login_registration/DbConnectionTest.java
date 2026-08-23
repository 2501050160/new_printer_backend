package com.saipraveen.login_registration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

public class DbConnectionTest {

    @Test
    public void testInspectCouponsColumns() {
        String url = "jdbc:postgresql://ep-silent-fog-azb5mawr-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_jShnM6rUD0lA";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected!");

            // 1. Drop the coupons table entirely
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS coupons CASCADE");
                System.out.println("DROPPED coupons table");
            }

            // 2. Recreate it fresh with exact columns matching the entity
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE coupons (" +
                    "  id BIGSERIAL PRIMARY KEY," +
                    "  coupon_code VARCHAR(255)," +
                    "  discount_percentage DOUBLE PRECISION DEFAULT 0," +
                    "  discount_amount DOUBLE PRECISION DEFAULT 0," +
                    "  min_order_amount DOUBLE PRECISION DEFAULT 0," +
                    "  expiry_date VARCHAR(255)," +
                    "  max_uses INTEGER DEFAULT 100," +
                    "  used_count INTEGER DEFAULT 0," +
                    "  active BOOLEAN DEFAULT true" +
                    ")"
                );
                System.out.println("CREATED fresh coupons table");
            }

            // 3. Insert a test coupon
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "INSERT INTO coupons (coupon_code, discount_percentage, discount_amount, min_order_amount, expiry_date, max_uses, used_count, active) " +
                    "VALUES ('TEST50', 50, 0, 0, '2026-12-31', 100, 0, true)"
                );
                System.out.println("INSERTED test coupon TEST50");
            }

            // 4. Verify
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'coupons' ORDER BY ordinal_position");
                System.out.println("\n=== NEW COUPONS TABLE SCHEMA ===");
                while (rs.next()) {
                    System.out.println("  " + rs.getString("column_name") + " | " + rs.getString("data_type"));
                }
            }

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM coupons");
                while (rs.next()) {
                    System.out.println("Row: id=" + rs.getLong("id") + " code=" + rs.getString("coupon_code") + " pct=" + rs.getDouble("discount_percentage") + " active=" + rs.getBoolean("active"));
                }
            }

    @Test
    public void testUsersTable() {
        String url = "jdbc:postgresql://ep-silent-fog-azb5mawr-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_jShnM6rUD0lA";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'users' ORDER BY ordinal_position");
                System.out.println("\n=== USERS TABLE SCHEMA ===");
                while (rs.next()) {
                    System.out.println("  " + rs.getString("column_name") + " | " + rs.getString("data_type") + " | nullable=" + rs.getString("is_nullable"));
                }
            }

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT id, name, email, wallet_balance, referral_code, blocked, email_verified, college FROM users WHERE id = 6");
                if (rs.next()) {
                    System.out.println("User 6: id=" + rs.getLong("id") + ", email=" + rs.getString("email") + ", balance=" + rs.getDouble("wallet_balance") + ", college=" + rs.getString("college"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
