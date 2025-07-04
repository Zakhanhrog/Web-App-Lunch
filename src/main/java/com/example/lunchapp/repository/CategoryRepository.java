package com.example.lunchapp.repository;

import com.example.lunchapp.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.foodItems")
    @Override
    List<Category> findAll();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.foodItems WHERE c.name = :name")
    Optional<Category> findByName(@Param("name") String name);
}