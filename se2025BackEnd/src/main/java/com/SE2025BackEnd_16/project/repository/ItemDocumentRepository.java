package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.ItemDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public interface ItemDocumentRepository extends ElasticsearchRepository<ItemDocument, String> {
    
    // 根据商品名称搜索
    List<ItemDocument> findByItemNameContaining(String itemName);
    
    // 根据描述搜索
    List<ItemDocument> findByDescriptionContaining(String description);
    
    // 根据分类名称搜索
    List<ItemDocument> findByCategoryName(String categoryName);
    
    // 根据价格范围搜索
    List<ItemDocument> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // 根据是否可用搜索
    List<ItemDocument> findByIsAvailable(Boolean isAvailable);
    
    // 根据是否删除搜索
    List<ItemDocument> findByIsDeleted(Boolean isDeleted);
    
    // 搜索可用且未删除的商品
    List<ItemDocument> findByIsAvailableTrueAndIsDeletedFalse();
} 