package com.example.lunchapp.service;

import com.example.lunchapp.model.entity.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<Category> getAllCategories();
    Optional<Category> findById(Long id);
    Category saveCategory(Category category);
    void deleteCategory(Long id);
    Optional<Category> findByName(String name);
    void createDefaultCategoriesIfNotExist();
}