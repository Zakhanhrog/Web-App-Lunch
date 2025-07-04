package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.model.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category")
    @Override
    List<FoodItem> findAll();

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category WHERE f.id = :id")
    @Override
    Optional<FoodItem> findById(@Param("id") Long id);

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category c WHERE c = :category")
    List<FoodItem> findByCategory(@Param("category") Category category);

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category WHERE lower(f.name) LIKE lower(concat('%', :name, '%'))")
    List<FoodItem> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category WHERE f.availableToday = true")
    List<FoodItem> findByAvailableTodayTrue();

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category c WHERE f.availableToday = true AND c = :category AND f.dailyQuantity > 0")
    List<FoodItem> findAvailableTodayByCategoryAndStock(@Param("category") Category category);

    @Query("SELECT f FROM FoodItem f LEFT JOIN FETCH f.category WHERE f.availableToday = true AND f.dailyQuantity > 0")
    List<FoodItem> findAllAvailableTodayWithStock();
}