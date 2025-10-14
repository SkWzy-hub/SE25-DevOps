package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // 根据分类名称查找
    Optional<Category> findByCategoryName(String categoryName);
} 