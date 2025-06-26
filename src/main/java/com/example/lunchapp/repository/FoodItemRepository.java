package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByCategory(Category category);

    List<FoodItem> findByNameContainingIgnoreCase(String name);

    // Lấy các món ăn được chọn cho menu hôm nay
    List<FoodItem> findByAvailableTodayTrue();

    // Lấy các món ăn cho menu hôm nay thuộc một category cụ thể
    @Query("SELECT f FROM FoodItem f WHERE f.availableToday = true AND f.category = :category AND f.dailyQuantity > 0")
    List<FoodItem> findAvailableTodayByCategoryAndStock(Category category);

    // Lấy tất cả các món ăn được đánh dấu availableToday = true và còn hàng (dailyQuantity > 0)
    @Query("SELECT f FROM FoodItem f WHERE f.availableToday = true AND f.dailyQuantity > 0")
    List<FoodItem> findAllAvailableTodayWithStock();
}