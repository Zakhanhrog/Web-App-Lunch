package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.repository.FoodItemRepository;
import com.example.lunchapp.service.FoodItemService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodItemServiceImpl implements FoodItemService {

    private final FoodItemRepository foodItemRepository;

    @Autowired
    public FoodItemServiceImpl(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getAvailableFoodItemsForToday() {
        List<FoodItem> foodItems = foodItemRepository.findAllAvailableTodayWithStock();
        foodItems.forEach(item -> {
            if (item.getCategory() != null) {
                Hibernate.initialize(item.getCategory());
            }
        });
        return foodItems;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Category, List<FoodItem>> getGroupedAvailableFoodItemsForToday() {
        List<FoodItem> foodItems = foodItemRepository.findAllAvailableTodayWithStock();
        foodItems.forEach(item -> {
            if (item.getCategory() != null) {
                Hibernate.initialize(item.getCategory());
            }
        });
        return foodItems.stream()
                .collect(Collectors.groupingBy(FoodItem::getCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoodItem> findById(Long id) {
        Optional<FoodItem> foodItemOpt = foodItemRepository.findById(id);
        foodItemOpt.ifPresent(foodItem -> {
            if (foodItem.getCategory() != null) {
                Hibernate.initialize(foodItem.getCategory());
            }
        });
        return foodItemOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getAllFoodItems() {
        List<FoodItem> foodItems = foodItemRepository.findAll();
        foodItems.forEach(item -> {
            if (item.getCategory() != null) {
                Hibernate.initialize(item.getCategory());
            }
        });
        return foodItems;
    }

    @Override
    @Transactional
    public FoodItem saveFoodItem(FoodItem foodItem) {
        return foodItemRepository.save(foodItem);
    }

    @Override
    @Transactional
    public void deleteFoodItem(Long id) {
        foodItemRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> findByCategory(Category category) {
        List<FoodItem> foodItems = foodItemRepository.findByCategory(category);
        foodItems.forEach(item -> {
            if (item.getCategory() != null) {
                Hibernate.initialize(item.getCategory());
            }
        });
        return foodItems;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> findByNameContaining(String name) {
        List<FoodItem> foodItems = foodItemRepository.findByNameContainingIgnoreCase(name);
        foodItems.forEach(item -> {
            if (item.getCategory() != null) {
                Hibernate.initialize(item.getCategory());
            }
        });
        return foodItems;
    }

    @Override
    @Transactional
    public void setFoodItemsForToday(List<Long> foodItemIds, Map<Long, Integer> dailyQuantities) {
        List<FoodItem> allItems = foodItemRepository.findAll();
        for (FoodItem item : allItems) {
            item.setAvailableToday(false);
            item.setDailyQuantity(0);
        }
        foodItemRepository.saveAll(allItems);

        if (foodItemIds != null && !foodItemIds.isEmpty()) {
            List<FoodItem> selectedItems = foodItemRepository.findAllById(foodItemIds);
            for (FoodItem item : selectedItems) {
                item.setAvailableToday(true);
                item.setDailyQuantity(dailyQuantities.getOrDefault(item.getId(), 0));
            }
            foodItemRepository.saveAll(selectedItems);
        }
    }

    @Override
    @Transactional
    public void resetDailyFoodItemStatus() {
        List<FoodItem> allItems = foodItemRepository.findAll();
        for (FoodItem item : allItems) {
            item.setAvailableToday(false);
            item.setDailyQuantity(0);
        }
        foodItemRepository.saveAll(allItems);
    }

    @Override
    @Transactional
    public void updateDailyMenuItemStatus(Long foodItemId, boolean isAvailable, int dailyQuantity) {
        FoodItem foodItem = foodItemRepository.findById(foodItemId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid food item ID: " + foodItemId));
        foodItem.setAvailableToday(isAvailable);
        foodItem.setDailyQuantity(dailyQuantity);
        foodItemRepository.save(foodItem);
    }
}