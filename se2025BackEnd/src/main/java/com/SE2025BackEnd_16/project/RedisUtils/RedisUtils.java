package com.SE2025BackEnd_16.project.RedisUtils;


import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SE2025BackEnd_16.project.dto.response.ItemResponseDTO;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.dto.converter.ItemConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.SE2025BackEnd_16.project.service.DistributedLockService;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;
import com.SE2025BackEnd_16.project.entity.Message;
import java.util.ArrayList;
import java.util.Map;

@Component
public class RedisUtils {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired(required = false)
    private DistributedLockService distributedLockService;

    // Redis Key 常量
    private static final String ITEM_DETAIL_KEY_PREFIX = "item:detail:";
    private static final String AVAILABLE_ITEMS_LIST_KEY = "items:available:list"; // String类型
    private static final String AVAILABLE_ITEMS_SET_KEY = "items:available:set";   // Set类型
    private static final String ALL_ITEMS_LIST_KEY = "items:all:list";             // String类型
    private static final String ITEM_IDS_SET_KEY = "items:ids:set";                // Set类型

    /**
     * 设置缓存
     */
    public void setCache(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonValue, 60, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            System.err.println("序列化缓存数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public String getCache(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }


    /**
     * 删除缓存
     */
    public void deleteCache(String key) {
        stringRedisTemplate.delete(key);
    }


    /**
     * 检查缓存是否存在
     */
    public boolean hasCache(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 设置缓存过期时间
     */
    public void expireCache(String key, long timeout, TimeUnit unit) {
        stringRedisTemplate.expire(key, timeout, unit);
    }
    public String generateBuyerKey(Integer userId,int page, int size){
        return "user:" + userId + ":purchased:page:" + page + ":size:" + size;
    }

    public String generateSellerKey(Integer userId, int page, int size){
        return "user:" + userId + ":seller:page:" + page + ":size:" + size;
    }

    public String generateOrderDetail(String orderId){
        return "order:" + orderId;
    }

    public String generateIteKey(int itemId){
        return "item:" + itemId;
    }

    // 批量删除买家分页缓存
    public void deleteAllBuyerPageCache(int buyerId) {
        // 假设分页最多100页，size为常用的5、10、20
        int[] sizes = {5, 10, 20};
        for (int size : sizes) {
            for (int page = 0; page < 100; page++) {
                String key = generateBuyerKey(buyerId, page, size);
                deleteCache(key);
            }
        }
    }

    // 批量删除卖家分页缓存
    public void deleteAllSellerPageCache(int sellerId) {
        int[] sizes = {5, 10, 20};
        for (int size : sizes) {
            for (int page = 0; page < 100; page++) {
                String key = generateSellerKey(sellerId, page, size);
                deleteCache(key);
            }
        }
    }

    // ==================== 商品缓存相关方法 ====================

    /**
     * 生成商品详情缓存Key
     */
    public String generateItemDetailKey(int itemId) {
        return ITEM_DETAIL_KEY_PREFIX + itemId;
    }

    /**
     * 缓存单个商品详情（缓存Item实体对象）
     * 并将商品ID加入所有排序字段的分类ZSet
     */
    public void cacheItemDetail(int itemId) {
        Item item = itemRepository.findByItemId(itemId);
        String key = generateItemDetailKey(itemId);
        setCache(key, item, 60, TimeUnit.MINUTES); // 缓存1小时
        // 将商品ID添加到商品ID集合中（Set类型）
        stringRedisTemplate.opsForSet().remove(ITEM_IDS_SET_KEY, String.valueOf(itemId));
        stringRedisTemplate.opsForSet().add(ITEM_IDS_SET_KEY, String.valueOf(itemId));
        // 如果商品可售，添加到可售商品ID集合
        if (item.getIsAvailable()) {
            stringRedisTemplate.opsForSet().remove(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
            stringRedisTemplate.opsForSet().add(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
        } else {
            stringRedisTemplate.opsForSet().remove(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
        }
        // 将商品ID加入分类集合
        stringRedisTemplate.opsForSet().add("category:" + item.getCategoryId() + ":items:set", String.valueOf(itemId));
        // 新增：将商品ID加入所有排序字段的分类ZSet
        double updateScore = item.getUpdateTime() != null
            ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            : System.currentTimeMillis() / 1000.0;
        double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
        double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
        addItemToCategorySortedSet(item.getCategoryId(), itemId, updateScore, "update_time");
        addItemToCategorySortedSet(item.getCategoryId(), itemId, priceScore, "price");
        addItemToCategorySortedSet(item.getCategoryId(), itemId, likesScore, "likes");
    }

    public void cacheItemDetail(int itemId, Item item) {
        setCache(generateItemDetailKey(itemId), item, 60, TimeUnit.MINUTES);
    }

    /**
     * 批量初始化所有分类商品集合和所有排序字段ZSet（只缓存可售商品）
     */
    public void cacheAllCategoryItems(List<Item> allItems) {
        // 先清空所有分类集合和所有ZSet
        Set<Integer> allCategoryIds = allItems.stream().map(Item::getCategoryId).collect(Collectors.toSet());
        String[] sortFields = {"update_time", "price", "likes"};
        for (Integer categoryId : allCategoryIds) {
            stringRedisTemplate.delete("category:" + categoryId + ":items:set");
            for (String sortBy : sortFields) {
                stringRedisTemplate.delete("category:" + categoryId + ":items:zset:" + sortBy);
            }
        }
        // 分类分组
        Map<Integer, List<Item>> categoryMap = allItems.stream().collect(Collectors.groupingBy(Item::getCategoryId));
        for (Map.Entry<Integer, List<Item>> entry : categoryMap.entrySet()) {
            Integer categoryId = entry.getKey();
            List<Item> items = entry.getValue();
            for (Item item : items) {
                if (Boolean.TRUE.equals(item.getIsAvailable())) { // 只缓存可售商品
                    stringRedisTemplate.opsForSet().add("category:" + categoryId + ":items:set", String.valueOf(item.getItemId()));
                    double updateScore = item.getUpdateTime() != null
                        ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                        : System.currentTimeMillis() / 1000.0;
                    double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                    double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
                    addItemToCategorySortedSet(categoryId, item.getItemId(), updateScore, "update_time");
                    addItemToCategorySortedSet(categoryId, item.getItemId(), priceScore, "price");
                    addItemToCategorySortedSet(categoryId, item.getItemId(), likesScore, "likes");
                }
            }
        }
    }

    /**
     * 获取某分类下所有商品ID
     */
    public Set<String> getCategoryItemIds(int categoryId) {
        return stringRedisTemplate.opsForSet().members("category:" + categoryId + ":items:set");
    }

    /**
     * 获取某分类下所有商品详情
     */
    public List<Item> getCategoryItems(int categoryId) {
        Set<String> ids = getCategoryItemIds(categoryId);
        List<Item> result = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                Item item = getItemDetail(Integer.parseInt(id));
                if (item != null) result.add(item);
            }
        }
        return result;
    }

    /**
     * 获取商品详情缓存（返回Item实体对象）
     */
    public Item getItemDetail(int itemId) {
        String key = generateItemDetailKey(itemId);
        String json = getCache(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, Item.class);
            } catch (JsonProcessingException e) {
                System.err.println("反序列化商品详情失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 删除商品详情缓存
     */
    public void deleteItemDetail(int itemId) {
        String key = generateItemDetailKey(itemId);
        deleteCache(key);
        
        // 从商品ID集合中移除
        stringRedisTemplate.opsForSet().remove(ITEM_IDS_SET_KEY, String.valueOf(itemId));
        
        // 从可售商品集合中移除
        stringRedisTemplate.opsForSet().remove(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
    }

    /**
     * 缓存可售商品列表（缓存Item实体对象列表，String类型）
     */
    public void cacheAvailableItems(List<Item> items) {
        setCache(AVAILABLE_ITEMS_LIST_KEY, items, 60, TimeUnit.MINUTES);
    }

    /**
     * 获取可售商品列表缓存（返回ItemResponseDTO列表，String类型）
     */
    public List<ItemResponseDTO> getAvailableItems() {
        String json = getCache(AVAILABLE_ITEMS_LIST_KEY);
        if (json != null) {
            try {
                List<Item> items = objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Item.class));
                return items.stream()
                    .map(this::convertItemToResponseDTO)
                    .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                System.err.println("反序列化可售商品列表失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 缓存所有商品列表（缓存Item实体对象列表，String类型）
     */
    public void cacheAllItems(List<Item> items) {
        setCache(ALL_ITEMS_LIST_KEY, items, 60, TimeUnit.MINUTES);
    }

    /**
     * 获取所有商品列表缓存（返回ItemResponseDTO列表，String类型）
     */
    public List<ItemResponseDTO> getAllItems() {
        String json = getCache(ALL_ITEMS_LIST_KEY);
        if (json != null) {
            try {
                List<Item> items = objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Item.class));
                return items.stream()
                    .map(this::convertItemToResponseDTO)
                    .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                System.err.println("反序列化所有商品列表失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 将Item对象转换为ItemResponseDTO对象（简化版本）
     * 用于Redis缓存场景，只包含Item对象中的基本信息
     */
    private ItemResponseDTO convertItemToResponseDTO(Item item) {
        return ItemResponseDTO.builder()
                .itemId(item.getItemId())
                .title(item.getItemName())
                .price(item.getPrice())
                .categoryName("") // 缓存中没有分类名称，需要后续补充
                .condition(item.getItemCondition())
                .description(item.getDescription())
                .imageUrls(List.of(item.getImageUrl())) // 将单个图片URL转换为列表
                .likes(item.getLikes())
                .isAvailable(item.getIsAvailable())
                .sellerId(item.getSellerId())
                .sellerName("") // 缓存中没有卖家名称，需要后续补充
                .updateTime(item.getUpdateTime())
                .createTime(item.getUpdateTime()) // 暂时使用updateTime
                .build();
    }

    /**
     * 获取所有商品ID集合（Set类型）
     */
    public Set<String> getAllItemIds() {
        return stringRedisTemplate.opsForSet().members(ITEM_IDS_SET_KEY);
    }

    /**
     * 获取可售商品ID集合（Set类型）
     */
    public Set<String> getAvailableItemIds() {
        return stringRedisTemplate.opsForSet().members(AVAILABLE_ITEMS_SET_KEY);
    }

    /**
     * 清除所有商品相关缓存
     */
    /**
     * 清除所有商品相关的Redis缓存
     */
    public void clearAllItemCache() {
        try {
            System.out.println("开始清除所有商品相关缓存...");
            
            // 1. 删除所有商品详情缓存
            Set<String> itemIds = getAllItemIds();
            if (itemIds != null && !itemIds.isEmpty()) {
                System.out.println("清除 " + itemIds.size() + " 个商品详情缓存");
                for (String itemId : itemIds) {
                    try {
                        deleteItemDetail(Integer.parseInt(itemId));
                    } catch (Exception e) {
                        System.err.println("清除商品详情缓存失败，itemId: " + itemId + ", 错误: " + e.getMessage());
                    }
                }
            }
            
            // 2. 删除基础集合缓存
            deleteCache(AVAILABLE_ITEMS_SET_KEY);
            deleteCache(ALL_ITEMS_LIST_KEY);
            deleteCache(ITEM_IDS_SET_KEY);
            deleteCache(AVAILABLE_ITEMS_LIST_KEY);
            deleteCache("item:all:set");
            
            // 3. 删除可售商品排序集合
            String[] sortFields = {"update_time", "price", "likes"};
            for (String sortBy : sortFields) {
                deleteCache("item:onsale:sorted:" + sortBy);
                deleteCache("item:all:sorted:" + sortBy);
            }
            
            // 4. 删除分类相关缓存
            // 获取所有可能的分类ID（这里假设分类ID范围是1-10，实际应该从数据库获取）
            for (int categoryId = 1; categoryId <= 10; categoryId++) {
                deleteCache("category:" + categoryId + ":items:set");
                for (String sortBy : sortFields) {
                    deleteCache("category:" + categoryId + ":items:zset:" + sortBy);
                }
            }
            
            // 5. 删除商品留言相关缓存
            if (itemIds != null) {
                for (String itemId : itemIds) {
                    deleteCache("item:" + itemId + ":comments:root");
                }
            }
            
            // 6. 删除卖家商品集合缓存
            if (itemIds != null) {
                for (String itemId : itemIds) {
                    try {
                        Item item = getItemDetail(Integer.parseInt(itemId));
                        if (item != null && item.getSellerId() != null) {
                            deleteCache("seller:" + item.getSellerId() + ":items:set");
                        }
                    } catch (Exception e) {
                        // 忽略错误，继续处理下一个
                    }
                }
            }
            
            System.out.println("所有商品相关缓存清除完成");
            
        } catch (Exception e) {
            System.err.println("清除商品相关缓存时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新商品状态（可售/不可售）
     */
    public void updateItemAvailability(int itemId, boolean isAvailable) {
        if (isAvailable) {
            stringRedisTemplate.opsForSet().remove(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
            stringRedisTemplate.opsForSet().add(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
        } else {
            stringRedisTemplate.opsForSet().remove(AVAILABLE_ITEMS_SET_KEY, String.valueOf(itemId));
        }
        // 清除相关列表缓存，强制重新加载
        deleteCache(AVAILABLE_ITEMS_LIST_KEY);
        deleteCache(ALL_ITEMS_LIST_KEY);
    }

    // ==================== 商品留言缓存相关方法 ====================


    public String generateMessageKey(int messageId){
        return "message:" + messageId;
    }
    /**
     * 新增留言
     * @param itemId 商品ID
     * @param message 留言对象
     */
    public void addMessage(int itemId, Message message) {
        String messageKey = "message:" + message.getMessageId();
        setCache(messageKey, message, 60, TimeUnit.MINUTES);
        if (message.getParentId() == null || message.getParentId() == 0) {
            // 根留言
            stringRedisTemplate.opsForSet().add("item:" + itemId + ":comments:root", String.valueOf(message.getMessageId()));
        } else {
            // 回复留言（只用parentId分组，不用itemId）
            stringRedisTemplate.opsForSet().add("parent:" + message.getParentId() + ":replies", String.valueOf(message.getMessageId()));
        }
    }

    /**
     * 删除留言
     * @param itemId 商品ID
     * @param messageId 留言ID
     * @param parentId 父留言ID
     */
    public void deleteMessage(int itemId, int messageId, int parentId) {
        String messageKey = "message:" + messageId;
        deleteCache(messageKey);
        if (parentId == 0) {
            stringRedisTemplate.opsForSet().remove("item:" + itemId + ":comments:root", String.valueOf(messageId));
        } else {
            stringRedisTemplate.opsForSet().remove("parent:" + parentId + ":replies", String.valueOf(messageId));
        }
    }

    /**
     * 获取商品根留言列表
     * @param itemId 商品ID
     * @return 根留言列表
     */
    public List<Message> getRootMessages(int itemId) {
        Set<String> ids = stringRedisTemplate.opsForSet().members("item:" + itemId + ":comments:root");
        return getMessagesByIds(ids);
    }

    /**
     * 获取某条留言的所有回复（只用parentId分组）
     * @param parentId 父留言ID
     * @return 回复留言列表
     */
    public List<Message> getReplyMessages(int parentId) {
        Set<String> ids = stringRedisTemplate.opsForSet().members("parent:" + parentId + ":replies");
        return getMessagesByIds(ids);
    }

    /**
     * 根据留言ID集合批量获取留言详情
     */
    private List<Message> getMessagesByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Message> result = new ArrayList<>();
        for (String id : ids) {
            String json = getCache("message:" + id);
            if (json != null) {
                try {
                    result.add(objectMapper.readValue(json, Message.class));
                } catch (Exception e) {
                    // 可加日志
                }
            }
        }
        return result;
    }

    /**
     * 批量缓存商品根留言
     * @param itemId 商品ID
     * @param messages 根留言列表
     */
    public void cacheRootMessages(int itemId, List<Message> messages) {
        if (messages == null) return;
        // 清空原有Set
        stringRedisTemplate.delete("item:" + itemId + ":comments:root");
        for (Message msg : messages) {
            setCache("message:" + msg.getMessageId(), msg, 60, TimeUnit.MINUTES);
            stringRedisTemplate.opsForSet().add("item:" + itemId + ":comments:root", String.valueOf(msg.getMessageId()));
        }
    }

    /**
     * 批量缓存某条留言的所有回复
     * @param parentId 父留言ID
     * @param replies 回复留言列表
     */
    public void cacheReplyMessages(int parentId, List<Message> replies) {
        if (replies == null) return;
        // 清空原有Set
        stringRedisTemplate.delete("parent:" + parentId + ":replies");
        for (Message msg : replies) {
            setCache("message:" + msg.getMessageId(), msg, 60, TimeUnit.MINUTES);
            stringRedisTemplate.opsForSet().add("parent:" + parentId + ":replies", String.valueOf(msg.getMessageId()));
        }
    }

    // ==================== 原有方法保持不变 ====================

    // 添加商品到分类Sorted Set，score为指定字段
    public void addItemToCategorySortedSet(int categoryId, int itemId, double score, String sortBy) {
        String key = "category:" + categoryId + ":items:zset:" + sortBy;
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(itemId), score);
    }

    // 获取分类下商品ID分页（按指定字段score排序）
    public List<Integer> getCategoryItemIdsSorted(int categoryId, int page, int size, boolean desc, String sortBy) {
        String key = "category:" + categoryId + ":items:zset:" + sortBy;
        long start = page * size;
        long end = start + size - 1;
        Set<String> idSet = desc
                ? stringRedisTemplate.opsForZSet().reverseRange(key, start, end)
                : stringRedisTemplate.opsForZSet().range(key, start, end);
        if (idSet == null) return List.of();
        return idSet.stream().map(Integer::parseInt).collect(Collectors.toList());
    }

    // 获取分类商品总数（按指定排序字段）
    public long getCategoryItemCount(int categoryId, String sortBy) {
        String key = "category:" + categoryId + ":items:zset:" + sortBy;
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }

    // ==================== 全局商品Sorted Set分页相关方法 ====================

    /**
     * 分页获取全局Sorted Set商品ID
     * @param page 页码
     * @param size 每页数量
     * @param desc 是否倒序
     * @param sortBy 排序字段（update_time/price/likes）
     * @return 商品ID列表
     */
    public List<Integer> getAllItemIdsSorted(int page, int size, boolean desc, String sortBy) {
        String key = "item:all:sorted:" + sortBy;
        long start = page * size;
        long end = start + size - 1;
        Set<String> idSet = desc
                ? stringRedisTemplate.opsForZSet().reverseRange(key, start, end)
                : stringRedisTemplate.opsForZSet().range(key, start, end);
        if (idSet == null) return List.of();
        return idSet.stream().map(Integer::parseInt).collect(Collectors.toList());
    }

    // ==================== 全部商品集合和可售商品集合分离 ====================
    /**
     * 全部商品集合（Set）
     */
    public void addItemToAllSet(int itemId) {
        stringRedisTemplate.opsForSet().add("item:all:set", String.valueOf(itemId));
    }
    /**
     * 可售商品Sorted Set（按指定字段排序）
     */
    public void addItemToOnSaleSortedSet(int itemId, double score, String sortBy) {
        String key = "item:onsale:sorted:" + sortBy;
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(itemId), score);
    }
    /**
     * 分页获取可售商品ID（按指定字段排序）
     */
    public List<Integer> getOnSaleItemIdsSorted(int page, int size, boolean desc, String sortBy) {
        String key = "item:onsale:sorted:" + sortBy;
        long start = page * size;
        long end = start + size - 1;
        Set<String> idSet = desc
                ? stringRedisTemplate.opsForZSet().reverseRange(key, start, end)
                : stringRedisTemplate.opsForZSet().range(key, start, end);
        if (idSet == null) return List.of();
        return idSet.stream().map(Integer::parseInt).collect(Collectors.toList());
    }
    /**
     * 获取可售商品总数
     */
    public long getOnSaleItemCount(String sortBy) {
        String key = "item:onsale:sorted:" + sortBy;
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }
    /**
     * 批量获取商品详情（只返回可售商品）
     */
    public List<Item> getItemsByIds(List<Integer> itemIds) {
        List<Item> result = new ArrayList<>();
        for (Integer id : itemIds) {
            Item item = getItemDetail(id);
            if (item == null) {
                item = itemRepository.findByItemId(id);
                if (item != null) {
                    setCache(generateItemDetailKey(id), item, 5, TimeUnit.MINUTES);
                }
            }
            // 只返回可售商品
            if (item != null && Boolean.TRUE.equals(item.getIsAvailable())) result.add(item);
        }
        return result;
    }

    /**
     * 获取全局Sorted Set商品总数
     */
    public long getAllItemCount(String sortBy) {
        String key = "item:all:sorted:" + sortBy;
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }

    /**
     * 将商品ID加入全局Sorted Set，score为指定字段
     */
    public void addItemToAllSortedSet(int itemId, double score, String sortBy) {
        String key = "item:all:sorted:" + sortBy;
        stringRedisTemplate.opsForZSet().add(key, String.valueOf(itemId), score);
    }

    /**
     * 从全局可售Sorted Set移除商品
     */
    public void removeItemFromOnSaleSortedSet(int itemId, String sortBy) {
        String key = "item:onsale:sorted:" + sortBy;
        stringRedisTemplate.opsForZSet().remove(key, String.valueOf(itemId));
    }
    /**
     * 从分类可售Sorted Set移除商品
     */
    public void removeItemFromCategorySortedSet(int categoryId, int itemId, String sortBy) {
        String key = "category:" + categoryId + ":items:zset:" + sortBy;
        stringRedisTemplate.opsForZSet().remove(key, String.valueOf(itemId));
    }

    public void addItemToCategorySet(int categoryId, int itemId) {
        stringRedisTemplate.opsForSet().add("category:" + categoryId + ":items:set", String.valueOf(itemId));
    }

    public void removeItemFromCategorySet(int categoryId, int itemId) {
        stringRedisTemplate.opsForSet().remove("category:" + categoryId + ":items:set", String.valueOf(itemId));
    }

    public void migrateItemCacheAndSets(int tempId, int realId, Item realItem) {
        // 1. 迁移缓存
        String tempKey = generateItemDetailKey(tempId);
        String realKey = generateItemDetailKey(realId);
        String json = getCache(tempKey);
        if (json != null) {
            setCache(realKey, realItem, 5, java.util.concurrent.TimeUnit.MINUTES);
            deleteCache(tempKey);
        }
        // 2. 替换所有集合中的ID
        // 分类集合
        stringRedisTemplate.opsForSet().remove("category:" + realItem.getCategoryId() + ":items:set", String.valueOf(tempId));
        stringRedisTemplate.opsForSet().add("category:" + realItem.getCategoryId() + ":items:set", String.valueOf(realId));
        // 卖家集合
        stringRedisTemplate.opsForSet().remove("seller:" + realItem.getSellerId() + ":items:set", String.valueOf(tempId));
        stringRedisTemplate.opsForSet().add("seller:" + realItem.getSellerId() + ":items:set", String.valueOf(realId));
        // 可售集合（如果商品可售）
        if (Boolean.TRUE.equals(realItem.getIsAvailable())) {
            stringRedisTemplate.opsForSet().add("items:available:set", String.valueOf(realId));
        }
        // 首页可售商品Sorted Set & 分类ZSet
        if (Boolean.TRUE.equals(realItem.getIsAvailable())) {
            double updateScore = realItem.getUpdateTime() != null ? realItem.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
            double priceScore = realItem.getPrice() != null ? realItem.getPrice().doubleValue() : 0.0;
            double likesScore = realItem.getLikes() != null ? realItem.getLikes() : 0.0;
            // 先移除所有临时ID
            removeItemFromCategorySortedSet(realItem.getCategoryId(), tempId, "update_time");
            removeItemFromCategorySortedSet(realItem.getCategoryId(), tempId, "price");
            removeItemFromCategorySortedSet(realItem.getCategoryId(), tempId, "likes");
            removeItemFromOnSaleSortedSet(tempId, "update_time");
            removeItemFromOnSaleSortedSet(tempId, "price");
            removeItemFromOnSaleSortedSet(tempId, "likes");
            // 再加入真实ID
            addItemToCategorySortedSet(realItem.getCategoryId(), realId, updateScore, "update_time");
            addItemToCategorySortedSet(realItem.getCategoryId(), realId, priceScore, "price");
            addItemToCategorySortedSet(realItem.getCategoryId(), realId, likesScore, "likes");
            addItemToOnSaleSortedSet(realId, updateScore, "update_time");
            addItemToOnSaleSortedSet(realId, priceScore, "price");
            addItemToOnSaleSortedSet(realId, likesScore, "likes");
        }
        // 其它集合可按需补充
    }

    /**
     * 添加商品到卖家商品集合
     */
    public void addItemToSellerSet(int sellerId, int itemId) {
        stringRedisTemplate.opsForSet().add("seller:" + sellerId + ":items:set", String.valueOf(itemId));
    }
    /**
     * 从卖家商品集合移除商品
     */
    public void removeItemFromSellerSet(int sellerId, int itemId) {
        stringRedisTemplate.opsForSet().remove("seller:" + sellerId + ":items:set", String.valueOf(itemId));
    }
    /**
     * 分页获取卖家商品ID（Set无序，适合小数据量，若需排序建议用Sorted Set）
     */
    public List<Integer> getSellerItemIds(int sellerId, int page, int size) {
        Set<String> idSet = stringRedisTemplate.opsForSet().members("seller:" + sellerId + ":items:set");
        if (idSet == null) return List.of();
        List<Integer> ids = idSet.stream().map(Integer::parseInt).collect(Collectors.toList());
        int from = Math.min(page * size, ids.size());
        int to = Math.min(from + size, ids.size());
        return ids.subList(from, to);
    }

    // ==================== 订单缓存相关方法 ====================

    /**
     * 缓存订单详情
     */
    public void cacheOrderDetail(String orderId, Order order) {
        String key = generateOrderDetail(orderId);
        setCache(key, order, 60, TimeUnit.MINUTES); // 订单缓存1小时
    }

    /**
     * 获取订单详情
     */
    public Order getOrderDetail(String orderId) {
        String key = generateOrderDetail(orderId);
        String json = getCache(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, Order.class);
            } catch (Exception e) {
                System.err.println("反序列化订单数据失败: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * 删除订单详情缓存
     */
    public void deleteOrderDetail(String orderId) {
        String key = generateOrderDetail(orderId);
        deleteCache(key);
    }

    /**
     * 添加订单到买家订单集合（按创建时间排序）
     */
    public void addOrderToBuyerSet(Integer buyerId, String orderId, Order order) {
        String key = "buyer:" + buyerId + ":orders:zset";
        // 使用订单创建时间作为score，确保按时间排序
        double score = order.getCreateTime() != null
            ? order.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            : System.currentTimeMillis() / 1000.0;
        stringRedisTemplate.opsForZSet().add(key, orderId, score);
    }

    /**
     * 添加订单到卖家订单集合（按创建时间排序）
     */
    public void addOrderToSellerSet(Integer sellerId, String orderId, Order order) {
        String key = "seller:" + sellerId + ":orders:zset";
        // 使用订单创建时间作为score，确保按时间排序
        double score = order.getCreateTime() != null
            ? order.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            : System.currentTimeMillis() / 1000.0;
        stringRedisTemplate.opsForZSet().add(key, orderId, score);
    }

    /**
     * 从买家订单集合移除订单
     */
    public void removeOrderFromBuyerSet(Integer buyerId, String orderId) {
        String key = "buyer:" + buyerId + ":orders:zset";
        stringRedisTemplate.opsForZSet().remove(key, orderId);
    }

    /**
     * 从卖家订单集合移除订单
     */
    public void removeOrderFromSellerSet(Integer sellerId, String orderId) {
        String key = "seller:" + sellerId + ":orders:zset";
        stringRedisTemplate.opsForZSet().remove(key, orderId);
    }

    /**
     * 分页获取买家订单ID（按创建时间倒序）
     */
    public List<String> getBuyerOrderIds(Integer buyerId, int page, int size) {
        String key = "buyer:" + buyerId + ":orders:zset";
        long start = page * size;
        long end = start + size - 1;
        Set<String> orderIds = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        return orderIds != null ? new ArrayList<>(orderIds) : List.of();
    }

    /**
     * 分页获取卖家订单ID（按创建时间倒序）
     */
    public List<String> getSellerOrderIds(Integer sellerId, int page, int size) {
        String key = "seller:" + sellerId + ":orders:zset";
        long start = page * size;
        long end = start + size - 1;
        Set<String> orderIds = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        return orderIds != null ? new ArrayList<>(orderIds) : List.of();
    }

    /**
     * 获取买家订单总数
     */
    public long getBuyerOrderCount(Integer buyerId) {
        String key = "buyer:" + buyerId + ":orders:zset";
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }

    /**
     * 获取卖家订单总数
     */
    public long getSellerOrderCount(Integer sellerId) {
        String key = "seller:" + sellerId + ":orders:zset";
        Long count = stringRedisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }

    /**
     * 批量获取订单详情
     */
    public List<Order> getOrdersByIds(List<String> orderIds) {
        List<Order> result = new ArrayList<>();
        for (String orderId : orderIds) {
            Order order = getOrderDetail(orderId);
            if (order != null) {
                result.add(order);
            }
        }
        return result;
    }

    /**
     * 清除买家所有订单缓存
     */
    public void clearBuyerOrderCache(Integer buyerId) {
        String key = "buyer:" + buyerId + ":orders:zset";
        stringRedisTemplate.delete(key);
    }

    // ==================== Redisson分布式锁相关方法 ====================

    /**
     * 原子性地检查和更新商品可售状态（使用Redisson）
     * @param itemId 商品ID
     * @param buyerId 买家ID
     * @return 是否成功获取商品（原子操作）
     */
    public boolean atomicCheckAndReserveItem(Integer itemId, Integer buyerId) {
        // 如果没有Redisson，使用简单的检查（非原子操作，仅用于开发环境）
        if (distributedLockService == null) {
            System.out.println("警告：Redisson未配置，使用非原子操作检查商品状态");
            
            // 获取商品详情
            Item item = getItemDetail(itemId);
            if (item == null || !Boolean.TRUE.equals(item.getIsAvailable())) {
                return false; // 商品不存在或已下架
            }
            
            // 非原子性地更新商品状态为不可售
            item.setIsAvailable(false);
            setCache(generateItemDetailKey(itemId), item, 5, TimeUnit.MINUTES);
            
            // 从可售商品集合中移除
            removeItemFromOnSaleSortedSet(itemId, "update_time");
            removeItemFromOnSaleSortedSet(itemId, "price");
            removeItemFromOnSaleSortedSet(itemId, "likes");
            
            // 从分类可售集合中移除
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "update_time");
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "price");
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "likes");
            
            return true; // 成功预留商品
        }
        
        String lockKey = "item:lock:" + itemId;
        
        try {
            // 使用Redisson尝试获取商品锁，等待3秒，持有锁30秒
            if (!distributedLockService.tryLock(lockKey, 3, 30, TimeUnit.SECONDS)) {
                return false; // 获取锁失败，说明有其他用户正在购买
            }
            
            // 获取商品详情
            Item item = getItemDetail(itemId);
            if (item == null || !Boolean.TRUE.equals(item.getIsAvailable())) {
                return false; // 商品不存在或已下架
            }
            
            // 原子性地更新商品状态为不可售
            item.setIsAvailable(false);
            setCache(generateItemDetailKey(itemId), item, 5, TimeUnit.MINUTES);
            
            // 从可售商品集合中移除
            removeItemFromOnSaleSortedSet(itemId, "update_time");
            removeItemFromOnSaleSortedSet(itemId, "price");
            removeItemFromOnSaleSortedSet(itemId, "likes");
            
            // 从分类可售集合中移除
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "update_time");
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "price");
            removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "likes");
            
            return true; // 成功预留商品
            
        } finally {
            // 释放锁
            distributedLockService.unlock(lockKey);
        }
    }

    /**
     * 使用Redisson获取分布式锁（兼容旧接口）
     * @param lockKey 锁的key
     * @param lockValue 锁的值（已废弃，保留兼容性）
     * @param expireTime 锁的过期时间（秒）
     * @return 是否成功获取锁
     * @deprecated 建议使用DistributedLockService.tryLock()
     */
    @Deprecated
    public boolean tryLock(String lockKey, String lockValue, long expireTime) {
        return distributedLockService.tryLock(lockKey, 3, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 使用Redisson释放分布式锁（兼容旧接口）
     * @param lockKey 锁的key
     * @param lockValue 锁的值（已废弃，保留兼容性）
     * @return 是否成功释放锁
     * @deprecated 建议使用DistributedLockService.unlock()
     */
    @Deprecated
    public boolean releaseLock(String lockKey, String lockValue) {
        distributedLockService.unlock(lockKey);
        return true;
    }

    /**
     * 清除卖家所有订单缓存
     */
    public void clearSellerOrderCache(Integer sellerId) {
        String key = "seller:" + sellerId + ":orders:zset";
        stringRedisTemplate.delete(key);
    }

    public List<Item> getAllItemsByIds(List<Integer> itemIds) {
        List<Item> result = new ArrayList<>();
        for (Integer id : itemIds) {
            Item item = getItemDetail(id);
            if (item == null) {
                item = itemRepository.findByItemId(id);
                if (item != null) {
                    setCache(generateItemDetailKey(id), item, 5, TimeUnit.MINUTES);
                }
            }
            if (item != null) result.add(item); // 不判断isAvailable
        }
        return result;
    }

    public void addItemToOnSaleSortedSets(int itemId, Item item) {
        double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
        double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
        double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
        addItemToOnSaleSortedSet(itemId, updateScore, "update_time");
        addItemToOnSaleSortedSet(itemId, priceScore, "price");
        addItemToOnSaleSortedSet(itemId, likesScore, "likes");
    }
}
