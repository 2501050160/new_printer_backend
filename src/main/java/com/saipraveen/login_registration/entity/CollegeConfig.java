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

    @Column(nullable = true)
    private String bankAccountNumber;

    @Column(nullable = true)
    private String bankIfsc;

    @Column(nullable = true)
    private String bankBeneficiaryName;

    @Column(nullable = true)
    private String razorpayAccountId; // Razorpay Route Linked Account ID (e.g. acc_xxxxxx)

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public String getBankBeneficiaryName() {
        return bankBeneficiaryName;
    }

    public void setBankBeneficiaryName(String bankBeneficiaryName) {
        this.bankBeneficiaryName = bankBeneficiaryName;
    }

    public String getRazorpayAccountId() {
        return razorpayAccountId;
    }

    public void setRazorpayAccountId(String razorpayAccountId) {
        this.razorpayAccountId = razorpayAccountId;
    }

    @Column(nullable = true)
    private String settlementEmail;

    @Column(nullable = true)
    private Boolean autoPayoutEnabled = false;

    @Column(nullable = true)
    private Double autoPayoutThreshold = 2000.0;

    @Column(nullable = true)
    private String autoPayoutSchedule = "THRESHOLD_IMMEDIATE";

    public String getSettlementEmail() {
        return settlementEmail;
    }

    public void setSettlementEmail(String settlementEmail) {
        this.settlementEmail = settlementEmail;
    }

    public Boolean getAutoPayoutEnabled() {
        return autoPayoutEnabled != null && autoPayoutEnabled;
    }

    public void setAutoPayoutEnabled(Boolean autoPayoutEnabled) {
        this.autoPayoutEnabled = autoPayoutEnabled;
    }

    public Double getAutoPayoutThreshold() {
        return autoPayoutThreshold != null ? autoPayoutThreshold : 2000.0;
    }

    public void setAutoPayoutThreshold(Double autoPayoutThreshold) {
        this.autoPayoutThreshold = autoPayoutThreshold;
    }

    public String getAutoPayoutSchedule() {
        return autoPayoutSchedule != null ? autoPayoutSchedule : "THRESHOLD_IMMEDIATE";
    }

    public void setAutoPayoutSchedule(String autoPayoutSchedule) {
        this.autoPayoutSchedule = autoPayoutSchedule;
    }
}

