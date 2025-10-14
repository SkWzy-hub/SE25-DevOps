package com.SE2025BackEnd_16.project.utils;


import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dao.ItemDao;
import com.SE2025BackEnd_16.project.dao.OrderDao;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
public class ItemsRedis {


    @Autowired
    private ItemDao itemDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private RedisUtils redisUtils;

    @PostConstruct
    public void preloadAllItemsToRedis() {
        try {
            System.out.println("开始预热商品缓存...");
            
            // 新增：预热前清除所有商品相关缓存
            clearAllItemRelatedCache();
            
            List<Item> allItems = itemDao.findAll();
            System.out.println("从数据库获取到商品数量: " + (allItems == null ? 0 : allItems.size()));

            // 新增：记录所有商品ID到文件
            StringBuilder sb = new StringBuilder();
            String outputFile = "all_items_loaded.txt";

            if (allItems != null && !allItems.isEmpty()) {
                int processedCount = 0;
                int availableCount = 0;

                for (Item item : allItems) {
                    try {
                        redisUtils.cacheItemDetail(item.getItemId());
                        // 全部商品集合
                        redisUtils.addItemToAllSet(item.getItemId());
                        // 只将可售商品加入可售Sorted Set
                        if (Boolean.TRUE.equals(item.getIsAvailable())) {
                            availableCount++;
                            double updateScore = item.getUpdateTime() != null
                                    ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                                    : System.currentTimeMillis() / 1000.0;
                            double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                            double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
                            redisUtils.addItemToOnSaleSortedSet(item.getItemId(), updateScore, "update_time");
                            redisUtils.addItemToOnSaleSortedSet(item.getItemId(), priceScore, "price");
                            redisUtils.addItemToOnSaleSortedSet(item.getItemId(), likesScore, "likes");
                        }
                        processedCount++;

                        // 新增：记录到StringBuilder
                        sb.append("itemId=").append(item.getItemId())
                          .append(", isAvailable=").append(item.getIsAvailable())
                          .append(", title=").append(item.getItemName())
                          .append(", updateTime=").append(item.getUpdateTime())
                          .append("\n");

                        if (processedCount % 100 == 0) {
                            System.out.println("已处理商品数量: " + processedCount);
                        }
                    } catch (Exception e) {
                        System.err.println("处理商品 " + item.getItemId() + " 时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                try {
                    redisUtils.cacheAllItems(allItems);
                    System.out.println("缓存所有商品列表完成");
                } catch (Exception e) {
                    System.err.println("缓存所有商品列表失败: " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    List<Item> availableItems = allItems.stream()
                            .filter(Item::getIsAvailable)
                            .toList();
                    redisUtils.cacheAvailableItems(availableItems);
                    System.out.println("缓存可售商品列表完成，可售商品数量: " + availableItems.size());
                } catch (Exception e) {
                    System.err.println("缓存可售商品列表失败: " + e.getMessage());
                    e.printStackTrace();
                }

                try {
                    redisUtils.cacheAllCategoryItems(allItems);
                    System.out.println("缓存分类商品完成");
                } catch (Exception e) {
                    System.err.println("缓存分类商品失败: " + e.getMessage());
                    e.printStackTrace();
                }

                // 新增：写入文件
                try {
                    java.nio.file.Files.write(
                        java.nio.file.Paths.get(outputFile),
                        sb.toString().getBytes(),
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                    );
                    System.out.println("已输出所有加载商品到 " + outputFile);
                } catch (Exception e) {
                    System.err.println("写入商品ID文件失败: " + e.getMessage());
                }

                System.out.println("商品缓存预热完成，总商品数: " + allItems.size() + 
                                 ", 可售商品数: " + availableCount + 
                                 ", 处理成功数: " + processedCount);
            } else {
                System.out.println("没有找到商品数据，跳过缓存预热");
            }
            // ========== 新增：订单买家/卖家ZSet预热 ==========
            System.out.println("开始预热订单买家/卖家ZSet...");
            List<Order> allOrders = orderDao.findAll();
            int buyerCount = 0, sellerCount = 0;
            for (Order order : allOrders) {
                if (order.getBuyer() != null) {
                    redisUtils.addOrderToBuyerSet(order.getBuyer().getUserId(), order.getOrderId(), order);
                    buyerCount++;
                }
                if (order.getSeller() != null) {
                    redisUtils.addOrderToSellerSet(order.getSeller().getUserId(), order.getOrderId(), order);
                    sellerCount++;
                }
            }
            System.out.println("订单买家/卖家ZSet预热完成，买家写入:" + buyerCount + ", 卖家写入:" + sellerCount + ", 总订单:" + allOrders.size());
            // ========== 新增结束 ==========
        } catch (Exception e) {
            System.err.println("商品缓存预热失败: " + e.getMessage());
            e.printStackTrace();
            // 不要抛出异常，避免阻止应用启动
        }
    }
    
    /**
     * 清除所有商品相关的Redis缓存
     */
    private void clearAllItemRelatedCache() {
        try {
            System.out.println("开始清除所有商品相关缓存...");
            redisUtils.clearAllItemCache();
            System.out.println("所有商品相关缓存清除完成");
        } catch (Exception e) {
            System.err.println("清除商品相关缓存时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
