package com.saipraveen.login_registration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "pdf_files")
public class PdfFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
private String paymentStatus;
    private String fileName;
private Long userId;
private String customerName;
private String blockLocation;
    private Integer copies;
    private String selectedPages;
    private String razorpayPaymentId;


    
    private String fileType;

    private Long fileSize;
private String status;
    private String orderId;

private LocalDateTime uploadTime;

private LocalDateTime paidAt;

private LocalDateTime cancelWindowEndsAt;

private LocalDateTime printingStartedAt;

private LocalDateTime finishedAt;

private LocalDateTime queuedAt;

private Double originalPrice;

private Double discountAmount;

private Double price;

private String printType;



    private Integer totalPages;

    private String otpCode;

    private String appliedReferralCode;

    private java.time.LocalDateTime scheduledTime;

    private String nupLayout = "1-up";
    private Boolean doubleSided = false;
    private String assignedPrinterName;

    private String googleDriveFileId;
    private String googleDriveWebViewLink;
    private LocalDateTime fileExpiryTime;
    private String orderChannel = "WEB";

    @Column(columnDefinition = "bytea")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] pdfData;

    public PdfFile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
   

public void setPaymentStatus1(String paymentStatus) {
    this.paymentStatus = paymentStatus;
}

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }
    public Integer getTotalPages() {
    return totalPages;
}

public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
}

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getPdfData() {
        return pdfData;
    }

    public void setPdfData(byte[] pdfData) {
        this.pdfData = pdfData;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getSelectedPages() {
        return selectedPages;
    }

    public void setSelectedPages(String selectedPages) {
        this.selectedPages = selectedPages;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(String blockLocation) {
        this.blockLocation = blockLocation;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCancelWindowEndsAt() {
        return cancelWindowEndsAt;
    }

    public void setCancelWindowEndsAt(LocalDateTime cancelWindowEndsAt) {
        this.cancelWindowEndsAt = cancelWindowEndsAt;
    }

    public LocalDateTime getPrintingStartedAt() {
        return printingStartedAt;
    }

    public void setPrintingStartedAt(LocalDateTime printingStartedAt) {
        this.printingStartedAt = printingStartedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(LocalDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getAppliedReferralCode() {
        return appliedReferralCode;
    }

    public void setAppliedReferralCode(String appliedReferralCode) {
        this.appliedReferralCode = appliedReferralCode;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public java.time.LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(java.time.LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getNupLayout() {
        return nupLayout == null ? "1-up" : nupLayout;
    }

    public void setNupLayout(String nupLayout) {
        this.nupLayout = nupLayout;
    }

    public Boolean getDoubleSided() {
        return doubleSided == null ? false : doubleSided;
    }

    public void setDoubleSided(Boolean doubleSided) {
        this.doubleSided = doubleSided;
    }

    public String getAssignedPrinterName() {
        return assignedPrinterName;
    }

    public void setAssignedPrinterName(String assignedPrinterName) {
        this.assignedPrinterName = assignedPrinterName;
    }

    public String getGoogleDriveFileId() {
        return googleDriveFileId;
    }

    public void setGoogleDriveFileId(String googleDriveFileId) {
        this.googleDriveFileId = googleDriveFileId;
    }

    public String getGoogleDriveWebViewLink() {
        return googleDriveWebViewLink;
    }

    public void setGoogleDriveWebViewLink(String googleDriveWebViewLink) {
        this.googleDriveWebViewLink = googleDriveWebViewLink;
    }

    public LocalDateTime getFileExpiryTime() {
        return fileExpiryTime;
    }

    public void setFileExpiryTime(LocalDateTime fileExpiryTime) {
        this.fileExpiryTime = fileExpiryTime;
    }

    public String getOrderChannel() {
        return orderChannel;
    }

    public void setOrderChannel(String orderChannel) {
        this.orderChannel = orderChannel;
    }
}
