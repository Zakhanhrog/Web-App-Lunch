package com.example.lunchapp.service;

import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FoodItemService {
    // Dùng cho User
    List<FoodItem> getAvailableFoodItemsForToday();
    Map<Category, List<FoodItem>> getGroupedAvailableFoodItemsForToday();
    Optional<FoodItem> findById(Long id);

    // Dùng cho Admin (sẽ bổ sung sau)
    List<FoodItem> getAllFoodItems();
    FoodItem saveFoodItem(FoodItem foodItem);
    void deleteFoodItem(Long id);
    List<FoodItem> findByCategory(Category category);
    List<FoodItem> findByNameContaining(String name);
    void setFoodItemsForToday(List<Long> foodItemIds, Map<Long, Integer> dailyQuantities);
    void resetDailyFoodItemStatus(); // Phương thức mới

}