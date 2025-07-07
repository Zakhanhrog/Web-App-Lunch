package com.example.lunchapp.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RevenueByDate {
    private LocalDate date;
    private BigDecimal totalRevenue;

    public RevenueByDate(LocalDate date, BigDecimal totalRevenue) {
        this.date = date;
        this.totalRevenue = totalRevenue;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}