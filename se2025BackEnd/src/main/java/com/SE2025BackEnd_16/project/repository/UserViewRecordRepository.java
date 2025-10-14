package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.UserViewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserViewRecordRepository extends JpaRepository<UserViewRecord, UserViewRecord.UserViewRecordId> {
    
    // 根据用户ID查询所有浏览记录
    List<UserViewRecord> findByUserId(Integer userId);
    
    // 根据分类ID查询所有浏览记录
    List<UserViewRecord> findByCategoryId(Integer categoryId);
    
    // 根据用户ID和分类ID查询特定记录
    Optional<UserViewRecord> findByUserIdAndCategoryId(Integer userId, Integer categoryId);
    
    // 检查是否存在特定用户和分类的记录
    boolean existsByUserIdAndCategoryId(Integer userId, Integer categoryId);
    
    // 查询用户浏览次数最多的分类
    @Query("SELECT uvr FROM UserViewRecord uvr WHERE uvr.userId = :userId ORDER BY uvr.categoryViewCounts DESC")
    List<UserViewRecord> findTopCategoriesByUser(@Param("userId") Integer userId);
    
    // 查询某分类的热门程度（总浏览次数）
    @Query("SELECT SUM(uvr.categoryViewCounts) FROM UserViewRecord uvr WHERE uvr.categoryId = :categoryId")
    Long getTotalViewsByCategoryId(@Param("categoryId") Integer categoryId);
    
    // 根据时间范围查询浏览记录
    List<UserViewRecord> findByViewTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    // 根据用户ID和时间范围查询浏览记录
    List<UserViewRecord> findByUserIdAndViewTimeBetween(Integer userId, LocalDateTime startTime, LocalDateTime endTime);
} 