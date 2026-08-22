package com.saipraveen.login_registration.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    // DataSource is auto-configured by Spring Boot from application.properties.
    // No custom bean needed - this avoids dual-datasource conflicts.
}
