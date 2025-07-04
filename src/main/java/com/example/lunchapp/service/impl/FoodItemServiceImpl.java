package com.example.lunchapp.service.impl;

import com.example.lunchapp.controller.AdminController;
import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import com.example.lunchapp.repository.FoodItemRepository;
import com.example.lunchapp.service.FoodItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FoodItemServiceImpl implements FoodItemService {

    private static final Logger logger = LoggerFactory.getLogger(FoodItemServiceImpl.class);
    private final FoodItemRepository foodItemRepository;

    @Autowired
    public FoodItemServiceImpl(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getAvailableFoodItemsForToday() {
        return foodItemRepository.findAllAvailableTodayWithStock();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Category, List<FoodItem>> getGroupedAvailableFoodItemsForToday() {
        List<FoodItem> foodItems = foodItemRepository.findAllAvailableTodayWithStock();
        return foodItems.stream()
                .collect(Collectors.groupingBy(FoodItem::getCategory));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoodItem> findById(Long id) {
        return foodItemRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> getAllFoodItems() {
        return foodItemRepository.findAll();
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
        return foodItemRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodItem> findByNameContaining(String name) {
        return foodItemRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional
    public void setFoodItemsForToday(List<Long> foodItemIds, Map<Long, Integer> dailyQuantities) {
        List<FoodItem> allItems = foodItemRepository.findAll();
        Set<Long> selectedIds = (foodItemIds != null) ? new HashSet<>(foodItemIds) : Collections.emptySet();

        for (FoodItem item : allItems) {
            if (selectedIds.contains(item.getId())) {
                item.setAvailableToday(true);
                item.setDailyQuantity(dailyQuantities.getOrDefault(item.getId(), 0));
            } else {
                item.setAvailableToday(false);
                item.setDailyQuantity(0);
            }
        }
        foodItemRepository.saveAll(allItems);
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

    @Override
    @Transactional
    public void updateDailyMenuItemsInBatch(List<AdminController.DailyMenuItemBatchUpdateRequest> updates) {
        if (updates == null) {
            logger.warn("Received a null list for batch update. No action taken.");
            return;
        }

        Map<Long, AdminController.DailyMenuItemBatchUpdateRequest> updateMap = updates.stream()
                .collect(Collectors.toMap(AdminController.DailyMenuItemBatchUpdateRequest::id, Function.identity()));

        List<FoodItem> allItems = foodItemRepository.findAll();

        for (FoodItem item : allItems) {
            AdminController.DailyMenuItemBatchUpdateRequest update = updateMap.get(item.getId());
            if (update != null) {
                item.setAvailableToday(update.available());
                item.setDailyQuantity(update.quantity());
            } else {
                item.setAvailableToday(false);
                item.setDailyQuantity(0);
            }
        }

        foodItemRepository.saveAll(allItems);
        logger.info("Batch update completed. Processed {} total items based on {} updates.", allItems.size(), updates.size());
    }
}