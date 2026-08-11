package com.saipraveen.login_registration.repository;

import java.time.LocalDateTime;

public interface PdfFileProjection {
    Long getId();
    Long getUserId();
    String getOrderId();
    String getFileName();
    Double getPrice();
    LocalDateTime getUploadTime();
    String getPaymentStatus();
    String getRazorpayPaymentId();
    String getStatus();
    String getPrintType();
    Integer getTotalPages();
    String getOtpCode();
    String getAppliedReferralCode();
    String getBlockLocation();
    LocalDateTime getCancelWindowEndsAt();
    LocalDateTime getQueuedAt();
    String getCustomerName();
    Integer getCopies();
    String getSelectedPages();
    String getFileType();
    Long getFileSize();
    Double getOriginalPrice();
    Double getDiscountAmount();
    LocalDateTime getScheduledTime();
    String getNupLayout();
    Boolean getDoubleSided();
    String getGoogleDriveFileId();
    String getGoogleDriveWebViewLink();
    LocalDateTime getFileExpiryTime();
}
