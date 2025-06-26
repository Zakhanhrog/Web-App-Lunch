package com.example.lunchapp.service.impl;

import com.example.lunchapp.model.entity.Category;
import com.example.lunchapp.repository.CategoryRepository;
import com.example.lunchapp.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private static final List<String> DEFAULT_CATEGORY_NAMES = Arrays.asList("Cơm", "Rau", "Đồ mặn");


    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Override
    @Transactional
    public void createDefaultCategoriesIfNotExist() {
        for (String categoryName : DEFAULT_CATEGORY_NAMES) {
            if (categoryRepository.findByName(categoryName).isEmpty()) {
                categoryRepository.save(new Category(categoryName));
            }
        }
    }
}