package com.saipraveen.login_registration.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {

    private static final String DEFAULT_NEON_JDBC = "jdbc:postgresql://ep-silent-fog-azb5mawr.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
    private static final String DEFAULT_NEON_USER = "neondb_owner";
    private static final String DEFAULT_NEON_PASS = "npg_jShnM6rUD0lA";

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = DEFAULT_NEON_JDBC;
        String username = DEFAULT_NEON_USER;
        String password = DEFAULT_NEON_PASS;

        String envUrl = System.getenv("NEON_DATABASE_URL");
        if (envUrl != null && envUrl.contains("neon.tech")) {
            jdbcUrl = envUrl.trim();
        }

        System.out.println("🔌 [DatabaseConfig] Connecting directly to NeonDB PostgreSQL at: " + jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(180000);
        config.setConnectionTimeout(30000);
        config.setInitializationFailTimeout(-1);
        config.setLeakDetectionThreshold(15000);
        config.setPoolName("NeonHikariPool");

        return new HikariDataSource(config);
    }
}
