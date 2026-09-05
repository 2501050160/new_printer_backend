package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "college_configs")
public class CollegeConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String collegeName;

    @Column(nullable = false)
    private String razorpayKeyId;

    @Column(nullable = false)
    private String razorpayKeySecret;

    @Column(nullable = true)
    private String whatsappBotPhone;

    @Column(nullable = true)
    private Boolean dedicatedBotEnabled = false;

    public CollegeConfig() {}

    public CollegeConfig(String collegeName, String razorpayKeyId, String razorpayKeySecret) {
        this.collegeName = collegeName;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    public CollegeConfig(String collegeName, String razorpayKeyId, String razorpayKeySecret, String whatsappBotPhone, Boolean dedicatedBotEnabled) {
        this.collegeName = collegeName;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.whatsappBotPhone = whatsappBotPhone;
        this.dedicatedBotEnabled = dedicatedBotEnabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    // Alias for JSON deserialization if payload uses "college" instead of "collegeName"
    public String getCollege() {
        return collegeName;
    }

    public void setCollege(String college) {
        this.collegeName = college;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public String getRazorpayKeySecret() {
        return razorpayKeySecret;
    }

    public void setRazorpayKeySecret(String razorpayKeySecret) {
        this.razorpayKeySecret = razorpayKeySecret;
    }

    public String getWhatsappBotPhone() {
        return whatsappBotPhone;
    }

    public void setWhatsappBotPhone(String whatsappBotPhone) {
        this.whatsappBotPhone = whatsappBotPhone;
    }

    public Boolean getDedicatedBotEnabled() {
        return dedicatedBotEnabled != null && dedicatedBotEnabled;
    }

    public void setDedicatedBotEnabled(Boolean dedicatedBotEnabled) {
        this.dedicatedBotEnabled = dedicatedBotEnabled;
    }

    @Column(nullable = true)
    private Boolean botLogoutRequested = false;

    public Boolean getBotLogoutRequested() {
        return botLogoutRequested != null && botLogoutRequested;
    }

    public void setBotLogoutRequested(Boolean botLogoutRequested) {
        this.botLogoutRequested = botLogoutRequested;
    }

    @Column(nullable = true)
    private String whatsappBotApiKey;

    public String getWhatsappBotApiKey() {
        return whatsappBotApiKey;
    }

    public void setWhatsappBotApiKey(String whatsappBotApiKey) {
        this.whatsappBotApiKey = whatsappBotApiKey;
    }
}
