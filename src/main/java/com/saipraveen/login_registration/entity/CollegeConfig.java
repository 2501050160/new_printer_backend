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

    public CollegeConfig() {}

    public CollegeConfig(String collegeName, String razorpayKeyId, String razorpayKeySecret) {
        this.collegeName = collegeName;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
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
}
