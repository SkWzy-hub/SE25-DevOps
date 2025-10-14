package com.SE2025BackEnd_16.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "items")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDocument {
    
    @Id
    private Integer itemId;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String itemName;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;
    
    @Field(type = FieldType.Double)
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal price;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String itemCondition;
    
    @Field(type = FieldType.Text)
    private String imageUrl;
    
    @Field(type = FieldType.Boolean)
    private Boolean isAvailable;
    
    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;
    
    @Field(type = FieldType.Integer)
    private Integer sellerId;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String sellerName;
    
    @Field(type = FieldType.Integer)
    private Integer categoryId;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String categoryName;
    
    @Field(type = FieldType.Keyword)
    private String updateTime;
    
    @Field(type = FieldType.Integer)
    private Integer likes;
    
    // 从Item实体转换为ItemDocument
    public static ItemDocument fromItem(Item item, String sellerName, String categoryName) {
        ItemDocument document = new ItemDocument();
        document.setItemId(item.getItemId());
        document.setItemName(item.getItemName());
        document.setDescription(item.getDescription());
        document.setPrice(item.getPrice());
        document.setItemCondition(item.getItemCondition());
        document.setImageUrl(item.getImageUrl());
        document.setIsAvailable(item.getIsAvailable());
        document.setIsDeleted(item.getIsDeleted());
        document.setSellerId(item.getSellerId());
        document.setSellerName(sellerName);
        document.setCategoryId(item.getCategoryId());
        document.setCategoryName(categoryName);
        
        // 将LocalDateTime转换为String格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        document.setUpdateTime(item.getUpdateTime() != null ? item.getUpdateTime().format(formatter) : null);
        
        document.setLikes(item.getLikes());
        return document;
    }
    
    // 便捷方法：获取完整的商品标题
    public String getTitle() {
        return this.itemName;
    }
    
    // 便捷方法：设置商品标题
    public void setTitle(String title) {
        this.itemName = title;
    }
    
    // 便捷方法：获取商品状态描述
    public String getCondition() {
        return this.itemCondition;
    }
    
    // 便捷方法：设置商品状态
    public void setCondition(String condition) {
        this.itemCondition = condition;
    }
} 