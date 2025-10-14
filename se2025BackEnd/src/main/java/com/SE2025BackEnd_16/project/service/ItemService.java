package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.dto.request.ItemCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemUpdateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemDeleteRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemToggleAvailabilityRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemQueryRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ItemResponseDTO;
import com.SE2025BackEnd_16.project.dto.response.PageResponseDTO;

import com.SE2025BackEnd_16.project.entity.Item;

import com.SE2025BackEnd_16.project.entity.UserInfo;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ItemService {
    
    /**
     * 发布商品
     */
    ItemResponseDTO createItem(ItemCreateRequestDTO requestDTO);
    
    /**
     * 根据ID获取商品详情
     */
    ItemResponseDTO getItemById(Integer itemId);
    
    /**
     * 获取所有商品
     */
    List<ItemResponseDTO> getAllItems();
    
    /**
     * 根据卖家ID获取商品
     */
    List<ItemResponseDTO> getItemsBySellerId(Integer sellerId);
    
    /**
     * 根据分类ID获取商品
     */
    List<ItemResponseDTO> getItemsByCategoryId(Integer categoryId);
    
    /**
     * 获取可售商品
     */
    List<ItemResponseDTO> getAvailableItems();
    
    /**
     * 更新商品信息（使用专门的UpdateRequestDTO）
     */
    ItemResponseDTO updateItem(Integer itemId, ItemUpdateRequestDTO requestDTO);
        
    /**
     * 上下架商品（使用专门的ToggleAvailabilityRequestDTO）
     */
    void toggleItemAvailability(Integer itemId, ItemToggleAvailabilityRequestDTO requestDTO);
    
    /**
     * 删除商品（使用专门的DeleteRequestDTO）
     */
    void deleteItem(Integer itemId, ItemDeleteRequestDTO requestDTO);
    
    /**
     * 恢复已删除的商品
     */
    void restoreItem(Integer itemId, Integer operatorId);
    
    /**
     * 获取用户的所有商品（包括已删除，用于管理界面）
     */
    List<ItemResponseDTO> getAllItemsBySellerIdIncludingDeleted(Integer sellerId);
    
    /**
     * 复杂查询商品（分页）
     */
    PageResponseDTO<ItemResponseDTO> queryItems(ItemQueryRequestDTO requestDTO);
    
    /**
<<<<<<< HEAD
     * 分类分页查询商品
     */
    PageResponseDTO<ItemResponseDTO> getItemsByCategoryPaged(Integer categoryId, int page, int size, String sortBy, String sortDirection);


    
    /**
=======
>>>>>>> origin/sql
     * 商品点赞（保留原有功能）
     */
    void likeItem(Integer itemId);
    
    /**
     * 取消点赞（保留原有功能）
     */
    void unlikeItem(Integer itemId);

    // ==================== 收藏相关方法 ====================
    
    /**
     * 检查用户是否已收藏商品
     */
    boolean checkItemFavoriteStatus(Integer itemId, Integer userId);
    
    /**
     * 获取商品收藏数量
     */
    long getItemFavoriteCount(Integer itemId);
    
    /**
     * 添加商品收藏
     */
    void addItemFavorite(Integer itemId, Integer userId);
    
    /**
     * 取消商品收藏
     */
    void removeItemFavorite(Integer itemId, Integer userId);
    
    /**
     * 获取用户收藏的商品列表
     */
    List<ItemResponseDTO> getUserFavoriteItems(Integer userId);
    
    /**
     * 获取收藏某商品的用户列表
     */
    List<UserInfo> getItemFavoriteUsers(Integer itemId);
    

    // ==================== 浏览记录相关方法 ====================
    
    /**
     * 获取用户浏览记录统计
     */
    List<Object> getUserViewRecords(Integer userId);
    
    /**
     * 获取热门分类（基于浏览记录）
     */
    List<Object> getPopularCategories();

} 