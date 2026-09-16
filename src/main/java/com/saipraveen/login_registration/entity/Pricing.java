package com.saipraveen.login_registration.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pricing")
public class Pricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String printType;

    private Double pricePerPage;

    private Double firstPagePrice;

    private String blockLocation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public Double getPricePerPage() {
        return pricePerPage;
    }

    public void setPricePerPage(Double pricePerPage) {
        this.pricePerPage = pricePerPage;
    }

    public Double getFirstPagePrice() {
        if (firstPagePrice != null && firstPagePrice > 0) {
            return firstPagePrice;
        }
        return pricePerPage != null ? pricePerPage : 0.0;
    }

    public void setFirstPagePrice(Double firstPagePrice) {
        this.firstPagePrice = firstPagePrice;
    }

    public String getBlockLocation() {
        return blockLocation;
    }

    public void setBlockLocation(String blockLocation) {
        this.blockLocation = blockLocation;
    }
}