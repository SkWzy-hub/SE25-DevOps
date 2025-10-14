package com.SE2025BackEnd_16.project.dao;

import com.SE2025BackEnd_16.project.entity.Item;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 商品数据访问接口
 */
public interface ItemDao {
    
    /**
     * 保存商品
     */
    Item save(Item item);
    
    /**
     * 根据ID查找商品（排除已删除）
     */
    Optional<Item> findById(Integer itemId);
    
    /**
     * 根据ID查找商品（包括已删除）
     */
    Optional<Item> findByIdIncludingDeleted(Integer itemId);
    
    /**
     * 查找所有商品（排除已删除）
     */
    List<Item> findAll();
    
    /**
<<<<<<< HEAD
     * 根据ID列表查找商品
=======
     * 根据ID列表批量查找商品（排除已删除）
>>>>>>> origin/sql
     */
    List<Item> findAllById(List<Integer> itemIds);
    
    /**
<<<<<<< HEAD
     * 根据卖家ID查找商品
=======
     * 根据卖家ID查找商品（排除已删除）
>>>>>>> origin/sql
     */
    List<Item> findBySellerId(Integer sellerId);
    
    /**
     * 根据卖家ID查找所有商品（包括已删除）
     */
    List<Item> findAllBySellerIdIncludingDeleted(Integer sellerId);
    
    /**
     * 根据分类ID查找商品
     */
    List<Item> findByCategoryId(Integer categoryId);
    
    /**
     * 根据可用状态查找商品
     */
    List<Item> findByIsAvailable(Boolean isAvailable);
    
    /**
     * 根据卖家ID和可用状态查找商品
     */
    List<Item> findBySellerIdAndIsAvailable(Integer sellerId, Boolean isAvailable);

    Page<Item> findAllPaged(Pageable pageable);
    Page<Item> findByIsAvailableTruePaged(Pageable pageable);
    Page<Item> findByCategoryIdPaged(Integer categoryId, Pageable pageable);

    

    /**
     * 更新商品可用状态
     */
    void updateAvailabilityStatus(Integer itemId, Boolean isAvailable);
    
    /**
     * 软删除商品
     */
    void softDeleteItem(Integer itemId);
    
    /**
     * 恢复已删除的商品
     */
    void restoreItem(Integer itemId);
    
    /**
     * 增加商品收藏数
     */
    void incrementLikes(Integer itemId);
    
    /**
     * 减少商品收藏数
     */
    void decrementLikes(Integer itemId);
    
    /**
     * 硬删除商品
     */
    void deleteById(Integer itemId);
} 