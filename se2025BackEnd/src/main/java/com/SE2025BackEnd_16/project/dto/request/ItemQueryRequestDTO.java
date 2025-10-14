package com.SE2025BackEnd_16.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * 商品查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemQueryRequestDTO {
    
    // 基本查询条件
    private String keyword; // 关键词搜索（标题、描述）
    private Integer categoryId; // 分类ID
    private Integer sellerId; // 卖家ID
    private Boolean isAvailable; // 是否可售
    
    // 价格筛选
    private BigDecimal minPrice; // 最低价格
    private BigDecimal maxPrice; // 最高价格
    
    // 新旧程度筛选
    private String condition; // 新旧程度
    
    // 排序条件
    private String sortBy = "update_time"; // 排序字段：update_time, price, likes
    private String sortDirection = "DESC"; // 排序方向：ASC, DESC
    
    // 分页条件
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0; // 页码，从0开始
    
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20; // 每页大小
    
    /**
     * 是否有关键词搜索
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }
    
    /**
     * 是否有价格筛选
     */
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }
    
    /**
     * 是否为有效的排序字段
     */
    public boolean isValidSortBy() {
        return "update_time".equals(sortBy) || 
               "price".equals(sortBy) || 
               "likes".equals(sortBy);
    }
    
    /**
     * 是否为降序排序
     */
    public boolean isDescending() {
        return "DESC".equalsIgnoreCase(sortDirection);
    }
    
    /**
     * 获取处理后的关键词（去除空格并转小写）
     */
    public String getProcessedKeyword() {
        return hasKeyword() ? keyword.trim().toLowerCase() : "";
    }
} 