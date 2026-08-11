package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "frontend_sections")
public class FrontendSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String sectionType; // "ADVERTISING", "NEW_BLOCK", "FEATURE"

    private String imageUrl;
    private String redirectUrl;

    private Boolean active = true;
    private Integer displayOrder = 0;

    public FrontendSection() {}

    public FrontendSection(String title, String content, String sectionType, String imageUrl, String redirectUrl, Boolean active, Integer displayOrder) {
        this.title = title;
        this.content = content;
        this.sectionType = sectionType;
        this.imageUrl = imageUrl;
        this.redirectUrl = redirectUrl;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSectionType() {
        return sectionType;
    }

    public void setSectionType(String sectionType) {
        this.sectionType = sectionType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    private String college = "ALL";

    public String getCollege() {
        return college == null ? "ALL" : college;
    }

    public void setCollege(String college) {
        this.college = college;
    }
}
