package com.SE2025BackEnd_16.project.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.ItemDocument;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class ElasticsearchSyncService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ItemSearchService itemSearchService;
    
    private LocalDateTime lastSyncTime = LocalDateTime.now().minusYears(1);
    
    /**
     * 1. 应用启动时初始化同步（简化版）
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeElasticsearchOnStartup() {
        try {
            log.info("🚀 应用启动，Elasticsearch准备就绪");
            
            // 延迟5秒让其他组件完全启动
            Thread.sleep(5000);
            
            // 检查是否需要初始同步
            if (isInitialSyncRequired()) {
                log.info("📥 检测到需要初始同步，开始全量同步...");
                itemSearchService.syncDataToElasticsearch();
                log.info("✅ 初始同步完成");
            } else {
                log.info("✅ Elasticsearch已有数据，跳过初始同步");
            }
            
        } catch (Exception e) {
            log.error("❌ 应用启动时ES初始化失败: " + e.getMessage());
        }
    }
    
    /**
     * 2. 定时增量同步（每30分钟执行一次）
     */
    @Scheduled(fixedRate = 1800000) // 30分钟 = 1800000ms
    @Async
    public void scheduledIncrementalSync() {
        try {
            log.info("⏰ 定时增量同步开始...");
            incrementalSyncToElasticsearch();
            log.info("✅ 定时增量同步完成");
        } catch (Exception e) {
            log.error("❌ 定时增量同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 3. 全量同步到Elasticsearch
     */
    @Transactional(readOnly = true)
    public CompletableFuture<String> fullSyncToElasticsearch() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔄 开始全量同步到Elasticsearch...");
                long startTime = System.currentTimeMillis();
                
                // 使用简化的同步方法
                itemSearchService.syncDataToElasticsearch();
                
                // 更新同步时间
                lastSyncTime = LocalDateTime.now();
                
                long endTime = System.currentTimeMillis();
                log.info("✅ 全量同步完成，耗时: " + (endTime - startTime) + "ms");
                
                return "✅ 全量同步成功";
                
            } catch (Exception e) {
                log.error("❌ 全量同步失败: " + e.getMessage());
                return "❌ 全量同步失败: " + e.getMessage();
            }
        });
    }
    
    /**
     * 4. 增量同步到Elasticsearch
     */
    @Transactional(readOnly = true)
    public CompletableFuture<String> incrementalSyncToElasticsearch() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔄 开始增量同步到Elasticsearch...");
                long startTime = System.currentTimeMillis();
                
                // 获取自上次同步后更新的商品
                List<Item> updatedItems = itemRepository.findByUpdateTimeAfter(lastSyncTime);
                
                if (updatedItems.isEmpty()) {
                    return "✅ 没有更新的商品数据需要同步";
                }
                
                // 批量同步
                String result = bulkSyncItems(updatedItems);
                
                // 更新同步时间
                lastSyncTime = LocalDateTime.now();
                
                long endTime = System.currentTimeMillis();
                log.info("✅ 增量同步完成，耗时: " + (endTime - startTime) + "ms");
                
                return result;
                
            } catch (Exception e) {
                log.error("❌ 增量同步失败: " + e.getMessage());
                return "❌ 增量同步失败: " + e.getMessage();
            }
        });
    }
    
    /**
     * 5. 批量同步商品（优化版：避免N+1查询）
     */
    private String bulkSyncItems(List<Item> items) {
        try {
            if (items.isEmpty()) {
                return "⚠️ 没有商品需要同步";
            }
            
            // 批量获取所有需要的用户和分类信息，避免N+1查询
            Set<Integer> sellerIds = items.stream()
                    .map(Item::getSellerId)
                    .collect(Collectors.toSet());
            
            Set<Integer> categoryIds = items.stream()
                    .map(Item::getCategoryId)
                    .collect(Collectors.toSet());
            
            // 批量查询用户信息
            Map<Integer, String> sellerNames = userInfoRepository.findAllById(sellerIds)
                    .stream()
                    .collect(Collectors.toMap(
                            user -> user.getUserId(),
                            user -> user.getUsername()
                    ));
            
            // 批量查询分类信息
            Map<Integer, String> categoryNames = categoryRepository.findAllById(categoryIds)
                    .stream()
                    .collect(Collectors.toMap(
                            category -> category.getCategoryId(),
                            category -> category.getCategoryName()
                    ));
            
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            for (Item item : items) {
                try {
                    // 从缓存的Map中获取信息，避免重复查询
                    String sellerName = sellerNames.getOrDefault(item.getSellerId(), "未知用户");
                    String categoryName = categoryNames.getOrDefault(item.getCategoryId(), "未分类");
                    
                    // 转换为ItemDocument
                    ItemDocument document = ItemDocument.fromItem(item, sellerName, categoryName);
                    
                    // 添加到批量请求
                    bulkBuilder.operations(op -> op
                            .index(idx -> idx
                                    .index("items")
                                    .id(String.valueOf(item.getItemId()))
                                    .document(document)
                            )
                    );
                } catch (Exception e) {
                    log.error("❌ 商品 " + item.getItemId() + " 转换失败: " + e.getMessage());
                    // 继续处理其他商品
                }
            }
            
            // 执行批量请求
            BulkResponse bulkResponse = elasticsearchClient.bulk(bulkBuilder.build());
            
            // 检查结果
            if (bulkResponse.errors()) {
                StringBuilder errorInfo = new StringBuilder("批量同步部分失败: ");
                for (BulkResponseItem item : bulkResponse.items()) {
                    if (item.error() != null) {
                        errorInfo.append(item.error().reason()).append("; ");
                    }
                }
                log.error(errorInfo.toString());
                return errorInfo.toString();
            }
            
            return "✅ 批量同步成功，共同步 " + items.size() + " 个商品";
            
        } catch (Exception e) {
            String error = "❌ 批量同步失败: " + e.getMessage();
            log.error(error);
            e.printStackTrace(); // 打印详细错误信息
            return error;
        }
    }
    
    /**
     * 6. 检查是否需要初始同步
     */
    private boolean isInitialSyncRequired() {
        try {
            // 检查ES中是否有数据
            var response = elasticsearchClient.count(c -> c.index("items"));
            long esCount = response.count();
            
            // 检查数据库中的可用且未删除商品数量
            long dbCount = itemRepository.countByIsAvailableTrueAndIsDeletedFalse();
            
            log.info("📊 ES中商品数量: " + esCount + ", 数据库中可用商品数量: " + dbCount);
            
            // 如果ES中没有数据，或者数据量相差很大，则需要初始同步
            return esCount == 0 || Math.abs(esCount - dbCount) > 5;
            
        } catch (Exception e) {
            log.error("❌ 检查初始同步状态失败: " + e.getMessage());
            return true; // 出错时默认需要同步
        }
    }
    
    /**
     * 7. 获取同步状态
     */
    public String getSyncStatus() {
        try {
            // 获取ES中的文档数量
            var response = elasticsearchClient.count(c -> c.index("items"));
            long esCount = response.count();
            
            // 获取数据库中的可用且未删除商品数量
            long dbCount = itemRepository.countByIsAvailableTrueAndIsDeletedFalse();
            
            // 安全处理lastSyncTime
            String lastSyncTimeStr = (lastSyncTime != null) ? lastSyncTime.toString() : "未同步";
            
            return String.format("📊 同步状态 - ES: %d条记录, 数据库可用商品: %d条记录, 上次同步: %s", 
                    esCount, dbCount, lastSyncTimeStr);
            
        } catch (Exception e) {
            return "❌ 获取同步状态失败: " + e.getMessage();
        }
    }
} 