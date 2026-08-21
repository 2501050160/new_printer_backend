package com.saipraveen.login_registration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

public class DbConnectionTest {

    @Test
    public void testInspectCouponsColumns() {
        String url = "jdbc:postgresql://ep-silent-fog-azb5mawr-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        String user = "neondb_owner";
        String pass = "npg_jShnM6rUD0lA";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("✅ Connected to Neon PostgreSQL!");

            // 1. Show ALL columns in coupons table
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'coupons' ORDER BY ordinal_position");
                System.out.println("\n=== COUPONS TABLE SCHEMA ===");
                while (rs.next()) {
                    System.out.println("  " + rs.getString("column_name") + " | " + rs.getString("data_type") + " | nullable=" + rs.getString("is_nullable"));
                }
            }

            // 2. Show ALL columns in rewards table (this one WORKS on Render)
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'rewards' ORDER BY ordinal_position");
                System.out.println("\n=== REWARDS TABLE SCHEMA (WORKING) ===");
                while (rs.next()) {
                    System.out.println("  " + rs.getString("column_name") + " | " + rs.getString("data_type") + " | nullable=" + rs.getString("is_nullable"));
                }
            }

            // 3. Try SELECT * FROM coupons to see actual data
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM coupons LIMIT 5");
                ResultSetMetaData md = rs.getMetaData();
                System.out.println("\n=== COUPONS DATA (up to 5 rows) ===");
                int colCount = md.getColumnCount();
                StringBuilder header = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    header.append(md.getColumnName(i)).append(" (").append(md.getColumnTypeName(i)).append(")");
                    if (i < colCount) header.append(" | ");
                }
                System.out.println(header);
                int rowNum = 0;
                while (rs.next()) {
                    rowNum++;
                    StringBuilder row = new StringBuilder("Row " + rowNum + ": ");
                    for (int i = 1; i <= colCount; i++) {
                        row.append(md.getColumnName(i)).append("=").append(rs.getString(i));
                        if (i < colCount) row.append(", ");
                    }
                    System.out.println(row);
                }
                if (rowNum == 0) System.out.println("  (no rows)");
            }

            // 4. Try the exact query that JPA findAll() would do
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT c.id, c.coupon_code, c.discount_percentage, c.discount_amount, c.min_order_amount, c.expiry_date, c.max_uses, c.used_count, c.active FROM coupons c");
                System.out.println("\n=== JPA-EQUIVALENT QUERY RESULT ===");
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("  id=" + rs.getString("id") + " code=" + rs.getString("coupon_code") + " pct=" + rs.getString("discount_percentage") + " amt=" + rs.getString("discount_amount") + " expiry=" + rs.getString("expiry_date") + " active=" + rs.getString("active"));
                }
                System.out.println("  Total coupons found: " + count);
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
