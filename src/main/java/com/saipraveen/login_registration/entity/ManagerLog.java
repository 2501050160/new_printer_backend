package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manager_logs")
public class ManagerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String managerName;
    private String college;
    private String actionType; // e.g. "PRICING_UPDATE", "MAINTENANCE_TOGGLE", "PAPER_COUNT_UPDATE"
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    private LocalDateTime timestamp;

    public ManagerLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ManagerLog(String managerName, String college, String actionType, String details) {
        this.managerName = managerName;
        this.college = college;
        this.actionType = actionType;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
