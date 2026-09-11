package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_records")
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String college;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime settlementDate;

    @Column(nullable = true)
    private String referenceId; // UTR Number, Bank Reference, or Razorpay Transfer ID (trf_xxx)

    @Column(nullable = false)
    private String paymentMode = "BANK_TRANSFER"; // BANK_TRANSFER, RAZORPAY_ROUTE, UPI, CHEQUE, CASH

    @Column(nullable = false)
    private String settledBy = "admin"; // Username of the main admin who settled

    @Column(length = 1000, nullable = true)
    private String notes;

    @Column(nullable = false)
    private String status = "COMPLETED"; // COMPLETED, PROCESSING, FAILED

    @Column(nullable = true)
    private Double balanceBeforeSettlement;

    @Column(nullable = true)
    private Double balanceAfterSettlement;

    public SettlementRecord() {
        this.settlementDate = LocalDateTime.now();
    }

    public SettlementRecord(String college, Double amount, String referenceId, String paymentMode, String settledBy, String notes, Double balanceBeforeSettlement, Double balanceAfterSettlement) {
        this.college = college;
        this.amount = amount;
        this.referenceId = referenceId;
        this.paymentMode = (paymentMode != null && !paymentMode.trim().isEmpty()) ? paymentMode : "BANK_TRANSFER";
        this.settledBy = (settledBy != null && !settledBy.trim().isEmpty()) ? settledBy : "admin";
        this.notes = notes;
        this.settlementDate = LocalDateTime.now();
        this.status = "COMPLETED";
        this.balanceBeforeSettlement = balanceBeforeSettlement;
        this.balanceAfterSettlement = balanceAfterSettlement;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public Double getAmount() {
        return amount != null ? amount : 0.0;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDateTime settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getSettledBy() {
        return settledBy;
    }

    public void setSettledBy(String settledBy) {
        this.settledBy = settledBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getBalanceBeforeSettlement() {
        return balanceBeforeSettlement;
    }

    public void setBalanceBeforeSettlement(Double balanceBeforeSettlement) {
        this.balanceBeforeSettlement = balanceBeforeSettlement;
    }

    public Double getBalanceAfterSettlement() {
        return balanceAfterSettlement;
    }

    public void setBalanceAfterSettlement(Double balanceAfterSettlement) {
        this.balanceAfterSettlement = balanceAfterSettlement;
    }
}
