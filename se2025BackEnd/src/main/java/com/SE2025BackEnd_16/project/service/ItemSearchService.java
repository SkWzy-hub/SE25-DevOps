package com.SE2025BackEnd_16.project.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.SE2025BackEnd_16.project.entity.Category;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.ItemDocument;
import com.SE2025BackEnd_16.project.repository.CategoryRepository;
import com.SE2025BackEnd_16.project.repository.ItemDocumentRepository;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ItemSearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ItemDocumentRepository itemDocumentRepository;

    /**
     * 超精确搜索（最大程度避免中文分词误匹配）
     */
    public List<ItemDocument> ultraPreciseSearchItems(String keyword, String category, 
                                                     Double minPrice, Double maxPrice,
                                                     String sortBy, String sortOrder,
                                                     int page, int size) {
        try {
            log.info("开始超精确搜索，关键词: " + keyword);
            
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            boolean hasSearchConditions = false;
            
            // 关键词搜索 - 优化权重配置，突出标题重要性
            if (keyword != null && !keyword.trim().isEmpty()) {
                Query keywordQuery = Query.of(q -> q
                    .bool(b -> b
                        // 标题匹配 - 最高权重
                        .should(s -> s.term(t -> t.field("itemName.keyword").value(keyword).boost(50.0f)))     // 标题完全匹配
                        .should(s -> s.matchPhrase(m -> m.field("itemName").query(keyword).boost(40.0f)))      // 标题短语匹配
                        .should(s -> s.wildcard(w -> w.field("itemName").value("*" + keyword + "*").boost(30.0f))) // 标题包含关键词
                        .should(s -> s.match(m -> m.field("itemName").query(keyword).boost(25.0f)))           // 标题分词匹配
                        
                        // 分类匹配 - 中等权重
                        .should(s -> s.term(t -> t.field("categoryName.keyword").value(keyword).boost(20.0f)))
                        .should(s -> s.matchPhrase(m -> m.field("categoryName").query(keyword).boost(18.0f)))
                        .should(s -> s.wildcard(w -> w.field("categoryName").value("*" + keyword + "*").boost(15.0f)))
                        .should(s -> s.match(m -> m.field("categoryName").query(keyword).boost(12.0f)))
                        
                        // 描述匹配 - 最低权重
                        .should(s -> s.matchPhrase(m -> m.field("description").query(keyword).boost(8.0f)))
                        .should(s -> s.wildcard(w -> w.field("description").value("*" + keyword + "*").boost(5.0f)))
                        .should(s -> s.match(m -> m.field("description").query(keyword).boost(3.0f)))
                        
                        .minimumShouldMatch("1")
                    )
                );
                boolQueryBuilder.must(keywordQuery);
                hasSearchConditions = true;
            }
            
            // 分类筛选
            if (category != null && !category.trim().isEmpty()) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("categoryName.keyword").value(category)));
                hasSearchConditions = true;
            }
            
            // 价格范围筛选
            if (minPrice != null && maxPrice != null) {
                boolQueryBuilder.filter(f -> f.range(r -> r.field("price").gte(JsonData.of(minPrice)).lte(JsonData.of(maxPrice))));
                hasSearchConditions = true;
            } else if (minPrice != null) {
                boolQueryBuilder.filter(f -> f.range(r -> r.field("price").gte(JsonData.of(minPrice))));
                hasSearchConditions = true;
            } else if (maxPrice != null) {
                boolQueryBuilder.filter(f -> f.range(r -> r.field("price").lte(JsonData.of(maxPrice))));
                hasSearchConditions = true;
            }
            
            // 只搜索可用且未删除的商品
            boolQueryBuilder.filter(f -> f.term(t -> t.field("isAvailable").value(true)));
            boolQueryBuilder.filter(f -> f.term(t -> t.field("isDeleted").value(false)));
            
            // 构建搜索请求
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index("items")
                    .from(page * size)
                    .size(size);
            
            // 设置查询
            if (!hasSearchConditions) {
                searchBuilder.query(q -> q.matchAll(m -> m));
            } else {
                searchBuilder.query(boolQueryBuilder.build()._toQuery());
            }
            
            // 排序 - 优先按相关性排序
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;
                searchBuilder.sort(s -> s.field(f -> f.field(sortBy).order(order)));
            } else {
                // 默认按相关性排序（评分），然后按更新时间
                searchBuilder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
                searchBuilder.sort(s -> s.field(f -> f.field("updateTime").order(SortOrder.Desc)));
            }
            
            SearchResponse<ItemDocument> response = elasticsearchClient.search(searchBuilder.build(), ItemDocument.class);
            
            List<ItemDocument> results = response.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
            
            log.info("超精确搜索完成，找到 " + results.size() + " 个商品，总命中数: " + response.hits().total().value());
            
            return results;
                    
        } catch (Exception e) {
            log.error("超精确搜索失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("超精确搜索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步数据到Elasticsearch（简化版）
     */
    public void syncDataToElasticsearch() {
        try {
            log.info("开始同步数据到Elasticsearch...");
            
            // 先检查ES连接
            try {
                var response = elasticsearchClient.ping();
                log.info("ES连接正常");
            } catch (Exception e) {
                log.error("ES连接失败: " + e.getMessage());
                throw new RuntimeException("ES连接失败", e);
            }
            
            // 获取所有可用且未删除的商品
            List<Item> items = itemRepository.findByIsAvailableTrueAndIsDeletedFalse();
            log.info("从数据库获取到 " + items.size() + " 个可用商品");
            
            if (items.isEmpty()) {
                log.info("没有可用商品数据需要同步");
                return;
            }
            
            // 清空现有索引（安全删除）
            try {
                log.info("正在清空ES索引...");
                itemDocumentRepository.deleteAll();
                Thread.sleep(2000); // 等待删除完成
                log.info("ES索引清空完成");
            } catch (Exception e) {
                log.error("清空索引失败，继续同步: " + e.getMessage());
            }
            
            // 转换为Document并保存到ES
            List<ItemDocument> itemDocuments = new ArrayList<>();
            log.info("开始转换商品数据...");
            
            for (Item item : items) {
                try {
                    // 获取卖家信息
                    String sellerName = userInfoRepository.findById(item.getSellerId())
                            .map(user -> user.getUsername())
                            .orElse("未知用户");
                    
                    // 获取分类信息
                    String categoryName = categoryRepository.findById(item.getCategoryId())
                            .map(category -> category.getCategoryName())
                            .orElse("未分类");
                    
                    // 转换为ItemDocument
                    ItemDocument document = ItemDocument.fromItem(item, sellerName, categoryName);
                    itemDocuments.add(document);
                    
                } catch (Exception e) {
                    log.error("转换商品 " + item.getItemId() + " 失败: " + e.getMessage());
                    // 继续处理其他商品
                }
            }
            
            log.info("转换完成，准备保存 " + itemDocuments.size() + " 个文档到ES");
            
            // 分批保存到ES，避免一次性保存太多数据
            int batchSize = 50;
            int totalSaved = 0;
            
            for (int i = 0; i < itemDocuments.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, itemDocuments.size());
                List<ItemDocument> batch = itemDocuments.subList(i, endIndex);
                
                try {
                    itemDocumentRepository.saveAll(batch);
                    totalSaved += batch.size();
                    log.info("已保存 " + totalSaved + "/" + itemDocuments.size() + " 个文档");
                    
                    // 短暂休息，避免过度压力
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    log.error("保存批次 " + (i/batchSize + 1) + " 失败: " + e.getMessage());
                    throw e; // 重新抛出异常
                }
            }
            
            log.info("数据同步完成，共同步 " + totalSaved + " 个商品");
            
            // 验证同步结果
            Thread.sleep(2000); // 等待ES索引完成
            try {
                long esCount = itemDocumentRepository.count();
                log.info("ES中现在有 " + esCount + " 个文档");
            } catch (Exception e) {
                log.error("验证同步结果失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("数据同步失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("数据同步失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 智能搜索（适用于各种场景）
     */
    public List<ItemDocument> smartSearchItems(String keyword, Integer categoryId, 
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             String sortBy, String sortOrder,
                                             int page, int size) {
        try {
            log.info("开始智能搜索，关键词: " + keyword);
            
            // 获取分类名称
            String categoryName = null;
            if (categoryId != null) {
                categoryName = categoryRepository.findById(categoryId)
                        .map(category -> category.getCategoryName())
                        .orElse(null);
            }
            
            // 调用超精确搜索
            return ultraPreciseSearchItems(
                    keyword, 
                    categoryName, 
                    minPrice != null ? minPrice.doubleValue() : null,
                    maxPrice != null ? maxPrice.doubleValue() : null,
                    sortBy, 
                    sortOrder, 
                    page, 
                    size
            );
            
        } catch (Exception e) {
            log.error("智能搜索失败: " + e.getMessage());
            e.printStackTrace();
            // 如果Elasticsearch搜索失败，返回空列表
            return List.of();
        }
    }
} 