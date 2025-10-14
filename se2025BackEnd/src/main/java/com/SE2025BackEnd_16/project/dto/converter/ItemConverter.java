package com.SE2025BackEnd_16.project.dto.converter;

import com.SE2025BackEnd_16.project.dto.request.ItemCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemUpdateRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ItemResponseDTO;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Category;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品转换器
 */
@Component
public class ItemConverter {
    
    /**
     * CreateRequestDTO转Entity
     */
    public Item toEntity(ItemCreateRequestDTO requestDTO, Integer categoryId, String imageUrl) {
        Item item = new Item();
        item.setItemName(requestDTO.getTitle());
        item.setPrice(new BigDecimal(requestDTO.getPrice()));
        item.setCategoryId(categoryId);
        item.setItemCondition(requestDTO.getCondition());
        item.setDescription(requestDTO.getDescription());
        item.setSellerId(requestDTO.getSellerId());
        
        // 设置图片URL
        if (imageUrl != null && !imageUrl.isEmpty()) {
            item.setImageUrl(imageUrl);
        }
        
        // 设置默认值
        item.setLikes(0);
        item.setIsAvailable(true);
        item.setUpdateTime(LocalDateTime.now());
        
        return item;
    }
    
    /**
     * UpdateRequestDTO转Entity（用于更新操作）
     */
    public void updateEntity(Item existingItem, ItemUpdateRequestDTO requestDTO, Integer categoryId, String newImageUrl) {
        existingItem.setItemName(requestDTO.getTitle());
        existingItem.setPrice(requestDTO.getPrice());
        existingItem.setCategoryId(categoryId);
        existingItem.setItemCondition(requestDTO.getCondition());
        existingItem.setDescription(requestDTO.getDescription());
        
        // 如果有新图片，更新图片URL
        if (newImageUrl != null && !newImageUrl.isEmpty()) {
            existingItem.setImageUrl(newImageUrl);
        }
        
        // 更新时间
        existingItem.setUpdateTime(LocalDateTime.now());
    }
    
    /**
     * Entity转ResponseDTO
     */
    public ItemResponseDTO toResponseDTO(Item item, String categoryName, String sellerName, List<String> imageUrls) {
        return ItemResponseDTO.builder()
                .itemId(item.getItemId())
                .title(item.getItemName())
                .price(item.getPrice())
                .categoryName(categoryName)
                .condition(item.getItemCondition())
                .description(item.getDescription())
                .imageUrls(imageUrls)
                .likes(item.getLikes())
                .isAvailable(item.getIsAvailable())
                .sellerId(item.getSellerId())
                .sellerName(sellerName)
                .updateTime(item.getUpdateTime())
                .createTime(item.getUpdateTime()) // 暂时使用updateTime
                .build();
    }
    
    /**
     * Entity列表转ResponseDTO列表
     */
    public List<ItemResponseDTO> toResponseDTOList(List<Item> items, 
                                                  List<String> categoryNames, 
                                                  List<String> sellerNames,
                                                  List<List<String>> imageUrlsList) {
        return items.stream()
                .map(item -> {
                    int index = items.indexOf(item);
                    String categoryName = index < categoryNames.size() ? categoryNames.get(index) : "";
                    String sellerName = index < sellerNames.size() ? sellerNames.get(index) : "";
                    List<String> imageUrls = index < imageUrlsList.size() ? imageUrlsList.get(index) : List.of();
                    return toResponseDTO(item, categoryName, sellerName, imageUrls);
                })
                .collect(Collectors.toList());
    }
} 