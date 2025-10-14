package com.SE2025BackEnd_16.project.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponseDTO {
    
    private Integer itemId;
    private String title;
    private BigDecimal price;
    private String categoryName;
    private String condition;
    private String description;
    private List<String> imageUrls;
    private Integer likes;
    private Boolean isAvailable;
    private Integer sellerId;
    private String sellerName;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    
    // 兼容性方法 - 处理单个图片URL
    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            this.imageUrls = List.of(imageUrl);
        }
    }
    
    public String getImageUrl() {
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
    }
} 