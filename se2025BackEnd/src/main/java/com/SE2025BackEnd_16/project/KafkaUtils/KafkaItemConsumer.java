package com.SE2025BackEnd_16.project.KafkaUtils;


import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.SE2025BackEnd_16.project.entity.Item;

@Component
public class KafkaItemConsumer {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaUtils kafkaUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @KafkaListener(topics = "ItemDetail", groupId = "Item")
    public void listen(String message) {
        try{
            System.out.println("收到缓存预热消息Item: " + message);
            Integer itemId = Integer.parseInt(message);
            redisUtils.cacheItemDetail(itemId);
        }
        catch(Exception e){
            e.printStackTrace();
            kafkaUtils.sendMessage("ItemDetail", message);
        }
    }

    @KafkaListener(topics = "favoriteItem", groupId = "Item")
    public void handleFavoriteItem(String message) {
        try {
            System.out.println("收到favoriteItem异步更新消息: " + message);
            Integer itemId = Integer.parseInt(message);
            // 查库获取最新item
            var itemOpt = itemRepository.findById(itemId);
            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                // 数据库likes+1
                int newLikes = (item.getLikes() == null ? 0 : item.getLikes()) + 1;
                item.setLikes(newLikes);
                itemRepository.save(item);
                // 更新ZSet
                double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
                double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                double likesScore = newLikes;
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, updateScore, "update_time");
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, priceScore, "price");
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, likesScore, "likes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("favoriteItem", message);
        }
    }

    @KafkaListener(topics = "unfavoriteItem", groupId = "Item")
    public void handleUnfavoriteItem(String message) {
        try {
            System.out.println("收到unfavoriteItem异步更新消息: " + message);
            Integer itemId = Integer.parseInt(message);
            // 查库获取最新item
            var itemOpt = itemRepository.findById(itemId);
            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                // 数据库likes-1
                int newLikes = (item.getLikes() == null ? 0 : item.getLikes()) - 1;
                if (newLikes < 0) newLikes = 0;
                item.setLikes(newLikes);
                itemRepository.save(item);
                // 更新ZSet
                double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
                double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                double likesScore = newLikes;
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, updateScore, "update_time");
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, priceScore, "price");
                redisUtils.addItemToCategorySortedSet(item.getCategoryId(), itemId, likesScore, "likes");
            }
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("unfavoriteItem", message);
        }
    }

    @KafkaListener(topics = "updateItem", groupId = "Item")
    public void handleUpdateItem(String message) {
        try {
            System.out.println("收到updateItem异步更新消息: " + message);
            String[] parts = message.split(",", -1);
            Integer itemId = Integer.parseInt(parts[0]);
            Integer oldCategoryId = Integer.parseInt(parts[1]);
            Integer newCategoryId = Integer.parseInt(parts[2]);
            String newImageUrl = parts.length > 3 ? parts[3] : null;
            var itemOpt = itemRepository.findById(itemId);
            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                // 其它字段同步（假设前端/缓存已保证数据正确）
                // 这里只做演示，实际应补全所有字段
                // 1. 图片
                if (newImageUrl != null && !newImageUrl.isEmpty()) {
                    item.setImageUrl(newImageUrl);
                }
                // 2. 标题、描述、价格、condition等（从缓存同步）
                Item cacheItem = redisUtils.getItemDetail(itemId);
                if (cacheItem != null) {
                    item.setItemName(cacheItem.getItemName());
                    item.setDescription(cacheItem.getDescription());
                    item.setPrice(cacheItem.getPrice());
                    item.setItemCondition(cacheItem.getItemCondition());
                    // 其它字段可补全
                }
                // 3. 分类变更
                if (!oldCategoryId.equals(newCategoryId)) {
                    // 从旧分类缓存集合移除
                    redisUtils.removeItemFromCategorySortedSet(oldCategoryId, itemId, "update_time");
                    redisUtils.removeItemFromCategorySortedSet(oldCategoryId, itemId, "price");
                    redisUtils.removeItemFromCategorySortedSet(oldCategoryId, itemId, "likes");
                    // 加入新分类缓存集合
                    double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
                    double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                    double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
                    redisUtils.addItemToCategorySortedSet(newCategoryId, itemId, updateScore, "update_time");
                    redisUtils.addItemToCategorySortedSet(newCategoryId, itemId, priceScore, "price");
                    redisUtils.addItemToCategorySortedSet(newCategoryId, itemId, likesScore, "likes");
                    item.setCategoryId(newCategoryId);
                }
                // 4. 保存数据库
                itemRepository.save(item);
                // 5. 更新商品详情缓存
                redisUtils.cacheItemDetail(itemId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("updateItem", message);
        }
    }

    @KafkaListener(topics = "sellerItemsCache", groupId = "Item")
    public void handleSellerItemsCache(String message) {
        try {
            System.out.println("收到sellerItemsCache异步预热消息: " + message);
            Integer sellerId = Integer.parseInt(message);
            var items = itemRepository.findBySellerId(sellerId);
            for (var item : items) {
                redisUtils.addItemToSellerSet(sellerId, item.getItemId());
                // 可选：预热商品详情缓存
                // redisUtils.cacheItemDetail(item.getItemId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("sellerItemsCache", message);
        }
    }

    @KafkaListener(topics = "toggleItemAvailability", groupId = "Item")
    public void handleToggleItemAvailability(String message) {
        try {
            System.out.println("收到toggleItemAvailability异步预热消息: " + message);
            String[] parts = message.split(",");
            Integer itemId = Integer.parseInt(parts[0]);
            Boolean isAvailable = Boolean.parseBoolean(parts[1]);
            // Integer operatorId = Integer.parseInt(parts[2]); // 可用于日志
            var itemOpt = itemRepository.findById(itemId);
            if (itemOpt.isPresent()) {
                var item = itemOpt.get();
                item.setIsAvailable(isAvailable);
                itemRepository.save(item);
                // 刷新缓存
                redisUtils.cacheItemDetail(itemId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("toggleItemAvailability", message); // 失败重试
        }
    }

    @KafkaListener(topics = "createItem", groupId = "Item")
    public void handleCreateItem(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            Item tempItem = mapper.readValue(message, Item.class);
            Integer tempId = tempItem.getItemId();
            tempItem.setItemId(null); // 让数据库自增
            Item savedItem = itemRepository.save(tempItem);
            Integer realId = savedItem.getItemId();
            // 迁移缓存
            redisUtils.migrateItemCacheAndSets(tempId, realId, savedItem);
        } catch (Exception e) {
            e.printStackTrace();
            kafkaUtils.sendMessage("createItem", message);
        }
    }
}
