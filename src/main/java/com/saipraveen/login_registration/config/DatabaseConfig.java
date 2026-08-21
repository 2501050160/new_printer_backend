package com.saipraveen.login_registration.config;

import java.net.URI;
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
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        String jdbcUrl = DEFAULT_NEON_JDBC;
        String username = DEFAULT_NEON_USER;
        String password = DEFAULT_NEON_PASS;

        if (rawUrl != null && !rawUrl.trim().isEmpty()) {
            String trimmed = rawUrl.trim();
            if (trimmed.startsWith("postgres://") || (trimmed.startsWith("postgresql://") && !trimmed.startsWith("jdbc:"))) {
                try {
                    String parseUriStr = trimmed;
                    if (parseUriStr.startsWith("postgres://")) {
                        parseUriStr = "postgresql://" + parseUriStr.substring("postgres://".length());
                    }
                    URI uri = new URI(parseUriStr);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "neondb";
                    
                    jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + path + "?sslmode=require";

                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length > 0 && !userInfo[0].isEmpty()) {
                            username = userInfo[0];
                        }
                        if (userInfo.length > 1 && !userInfo[1].isEmpty()) {
                            password = userInfo[1];
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse DATABASE_URL URI, falling back to NeonDB: " + e.getMessage());
                    jdbcUrl = DEFAULT_NEON_JDBC;
                    username = DEFAULT_NEON_USER;
                    password = DEFAULT_NEON_PASS;
                }
            } else if (trimmed.startsWith("jdbc:postgresql:")) {
                jdbcUrl = trimmed;
                String envUser = System.getenv("DATABASE_USERNAME");
                if (envUser != null && !envUser.isEmpty()) username = envUser;
                String envPass = System.getenv("DATABASE_PASSWORD");
                if (envPass != null && !envPass.isEmpty()) password = envPass;
            }
        }

        System.out.println("🔌 [DatabaseConfig] Connecting to PostgreSQL at: " + jdbcUrl.replaceAll(":[^:@]+@", ":***@"));

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
