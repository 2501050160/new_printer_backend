package com.saipraveen.login_registration.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = System.getenv("DATABASE_URL");
        String url;
        String user = "neondb_owner";
        String pass = "npg_jShnM6rUD0lA";

        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            String temp = dbUrl.trim();
            if (temp.startsWith("postgres://")) {
                temp = "jdbc:postgresql://" + temp.substring("postgres://".length());
            } else if (temp.startsWith("postgresql://")) {
                temp = "jdbc:postgresql://" + temp.substring("postgresql://".length());
            } else if (!temp.startsWith("jdbc:postgresql://")) {
                temp = "jdbc:postgresql://" + temp;
            }
            if (!temp.contains("?")) {
                temp += "?sslmode=require";
            } else if (!temp.contains("sslmode=")) {
                temp += "&sslmode=require";
            }
            url = temp;
        } else {
            url = "jdbc:postgresql://ep-silent-fog-azb5mawr-pooler.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setInitializationFailTimeout(-1); // Resilient startup even if NeonDB takes a second to wake up

        return new HikariDataSource(config);
    }
}
