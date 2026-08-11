
package com.saipraveen.login_registration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "printer_config")
public class PrinterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockLocation;

    private String printerName;

    private String printerIp;

    private Boolean active;

    private Boolean maintenance = false;

    private Integer paperCount = 500;

    private Boolean qrScanToPrint = false;

    private Boolean otpEnabled = true;

    public PrinterConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(String blockLocation) {
        this.blockLocation = blockLocation;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public String getPrinterIp() {
        return printerIp;
    }

    public void setPrinterIp(String printerIp) {
        this.printerIp = printerIp;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(Boolean maintenance) {
        this.maintenance = maintenance;
    }

    public Integer getPaperCount() {
        return paperCount;
    }

    public void setPaperCount(Integer paperCount) {
        this.paperCount = paperCount;
    }

    public Boolean getQrScanToPrint() {
        return qrScanToPrint != null ? qrScanToPrint : false;
    }

    public void setQrScanToPrint(Boolean qrScanToPrint) {
        this.qrScanToPrint = qrScanToPrint;
    }

    public Boolean getOtpEnabled() {
        return otpEnabled != null ? otpEnabled : true;
    }

    public void setOtpEnabled(Boolean otpEnabled) {
        this.otpEnabled = otpEnabled;
    }

    private Boolean colourSupported = false;

    // Getter and setter for colourSupported
    public Boolean getColourSupported() {
        return colourSupported != null ? colourSupported : false;
    }

    public void setColourSupported(Boolean colourSupported) {
        this.colourSupported = colourSupported;
    }

    private Boolean paused = false;

    public Boolean getPaused() {
        return paused != null ? paused : false;
    }

    public void setPaused(Boolean paused) {
        this.paused = paused;
    }

    private Boolean bwDisabledForColor = false;

    public Boolean getBwDisabledForColor() {
        return bwDisabledForColor != null ? bwDisabledForColor : false;
    }

    public void setBwDisabledForColor(Boolean bwDisabledForColor) {
        this.bwDisabledForColor = bwDisabledForColor;
    }
}