package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    
    // 通过用户ID查询收藏
    List<Favorite> findByUserId(Integer userId);
    
    // 通过物品ID查询收藏
    List<Favorite> findByItemId(Integer itemId);
    
    // 检查用户是否已收藏某物品
    boolean existsByUserIdAndItemId(Integer userId, Integer itemId);
    
    // 删除用户对某物品的收藏
    void deleteByUserIdAndItemId(Integer userId, Integer itemId);
    
    // 统计物品的收藏数量
    long countByItemId(Integer itemId);
    
    // 统计用户的收藏数量
    long countByUserId(Integer userId);
} 