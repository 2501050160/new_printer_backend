package com.saipraveen.login_registration.dto;

public class OrderRequest {

    private String orderId;
    private Integer copies;
    private String selectedPages;
    private String printType;
    private Boolean doubleSided;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public String getSelectedPages() {
        return selectedPages;
    }

    public void setSelectedPages(String selectedPages) {
        this.selectedPages = selectedPages;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public Boolean getDoubleSided() {
        return doubleSided;
    }

    public void setDoubleSided(Boolean doubleSided) {
        this.doubleSided = doubleSided;
    }
}