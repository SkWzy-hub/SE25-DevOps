package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    
    // 重写基础方法，过滤已删除的商品
    @Query("SELECT i FROM Item i WHERE i.isDeleted = false")
    List<Item> findAll();
    
    @Query("SELECT i FROM Item i WHERE i.itemId = :itemId AND i.isDeleted = false")
    Optional<Item> findById(@Param("itemId") Integer itemId);

    
    // 通过卖家ID查询用户发布的物品（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.sellerId = :sellerId AND i.isDeleted = false")
    List<Item> findBySellerId(@Param("sellerId") Integer sellerId);
    
    // 通过分类ID查询物品（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.categoryId = :categoryId AND i.isDeleted = false")
    List<Item> findByCategoryId(@Param("categoryId") Integer categoryId);
    
    // 查询可售物品（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.isAvailable = true AND i.isDeleted = false")
    List<Item> findByIsAvailableTrue();
    
    // 根据价格范围查询物品（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.price BETWEEN :minPrice AND :maxPrice AND i.isDeleted = false")
    List<Item> findByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);


    Item findByItemId(Integer itemId);


    // 根据物品名称模糊查询（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.itemName LIKE %:itemName% AND i.isDeleted = false")
    List<Item> findByItemNameContainingIgnoreCase(@Param("itemName") String itemName);

    
    // 根据可用状态查询（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.isAvailable = :isAvailable AND i.isDeleted = false")
    List<Item> findByIsAvailable(@Param("isAvailable") Boolean isAvailable);
    
    // 根据卖家ID和可用状态查询（排除已删除）
    @Query("SELECT i FROM Item i WHERE i.sellerId = :sellerId AND i.isAvailable = :isAvailable AND i.isDeleted = false")
    List<Item> findBySellerIdAndIsAvailable(@Param("sellerId") Integer sellerId, @Param("isAvailable") Boolean isAvailable);
    
    // 更新商品可用状态
    @Modifying
    @Query("UPDATE Item i SET i.isAvailable = :isAvailable WHERE i.itemId = :itemId AND i.isDeleted = false")
    void updateAvailabilityStatus(@Param("itemId") Integer itemId, @Param("isAvailable") Boolean isAvailable);
    
    // 软删除商品
    @Modifying
    @Query("UPDATE Item i SET i.isDeleted = true WHERE i.itemId = :itemId")
    void softDeleteItem(@Param("itemId") Integer itemId);
    
    // 恢复已删除的商品
    @Modifying
    @Query("UPDATE Item i SET i.isDeleted = false WHERE i.itemId = :itemId")
    void restoreItem(@Param("itemId") Integer itemId);
    
    // 查询用户的所有商品（包括已删除，用于管理界面）
    @Query("SELECT i FROM Item i WHERE i.sellerId = :sellerId")
    List<Item> findAllBySellerIdIncludingDeleted(@Param("sellerId") Integer sellerId);
    
    // 增加收藏数（只对未删除的商品）
    @Modifying
    @Query("UPDATE Item i SET i.likes = i.likes + 1 WHERE i.itemId = :itemId AND i.isDeleted = false")
    void incrementLikes(@Param("itemId") Integer itemId);
    
    // 减少收藏数（只对未删除的商品）
    @Modifying
    @Query("UPDATE Item i SET i.likes = i.likes - 1 WHERE i.itemId = :itemId AND i.likes > 0 AND i.isDeleted = false")
    void decrementLikes(@Param("itemId") Integer itemId);


    // 全部商品分页
    Page<Item> findAll(Pageable pageable);
    // 可售商品分页
    Page<Item> findByIsAvailableTrue(Pageable pageable);
    // 分类分页
    Page<Item> findByCategoryId(Integer categoryId, Pageable pageable);

    
    // ========== Elasticsearch同步相关方法 ==========
    
    // 查询可用且未删除的物品
    @Query("SELECT i FROM Item i WHERE i.isAvailable = true AND i.isDeleted = false")
    List<Item> findByIsAvailableTrueAndIsDeletedFalse();
    
    // 统计可用且未删除的物品数量
    @Query("SELECT COUNT(i) FROM Item i WHERE i.isAvailable = true AND i.isDeleted = false")
    long countByIsAvailableTrueAndIsDeletedFalse();
    
    // 查询指定时间之后更新的物品（用于增量同步）
    @Query("SELECT i FROM Item i WHERE i.updateTime > :updateTime")
    List<Item> findByUpdateTimeAfter(@Param("updateTime") LocalDateTime updateTime);
    
    // 查询所有未删除的物品（用于Elasticsearch同步）
    @Query("SELECT i FROM Item i WHERE i.isDeleted = false")
    List<Item> findByIsDeletedFalse();

} 