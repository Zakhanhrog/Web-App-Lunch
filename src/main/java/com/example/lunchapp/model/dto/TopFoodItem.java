package com.example.lunchapp.model.dto;

public class TopFoodItem {
    private String foodName;
    private Long totalQuantity;

    public TopFoodItem(String foodName, Long totalQuantity) {
        this.foodName = foodName;
        this.totalQuantity = totalQuantity;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
}