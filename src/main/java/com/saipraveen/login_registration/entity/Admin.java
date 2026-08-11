package com.saipraveen.login_registration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String role = "SUB_ADMIN";

    private String college = "KLU";

    private String managerSecret;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        if ("admin".equalsIgnoreCase(username)) {
            return "MAIN_ADMIN";
        }
        return role == null ? "SUB_ADMIN" : role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCollege() {
        if ("admin".equalsIgnoreCase(username)) {
            return "ALL";
        }
        return college == null ? "KLU" : college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getManagerSecret() {
        return managerSecret;
    }

    public void setManagerSecret(String managerSecret) {
        this.managerSecret = managerSecret;
    }
}