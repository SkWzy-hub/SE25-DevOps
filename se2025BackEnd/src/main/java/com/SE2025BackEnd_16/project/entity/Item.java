package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;
    
    @Column(name = "image_url", nullable = false, length = 200)
    private String imageUrl;
    
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "item_condition", nullable = false, length = 20)
    private String itemCondition;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "likes")
    private Integer likes = 0;
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "seller_id", nullable = false)
    private Integer sellerId;
    
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
    
    // 移除List集合，通过Repository查询获取物品相关的订单、留言、收藏
    
    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        updateTime = LocalDateTime.now();
    }
    
    // 兼容性方法 - 为新的搜索功能提供getter方法
    public String getTitle() {
        return this.itemName;
    }
    
    public String getCondition() {
        return this.itemCondition;
    }
    
    public void setTitle(String title) {
        this.itemName = title;
    }
    
    public void setCondition(String condition) {
        this.itemCondition = condition;
    }
} 