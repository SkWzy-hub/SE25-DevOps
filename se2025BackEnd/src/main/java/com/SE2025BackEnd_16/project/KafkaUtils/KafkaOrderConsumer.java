package com.SE2025BackEnd_16.project.KafkaUtils;

import com.SE2025BackEnd_16.project.dao.OrderDao;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class KafkaOrderConsumer {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private RedisUtils redisUtils;

    // 处理创建订单消息
    @KafkaListener(topics = "createOrder", groupId = "order-group")
    @Transactional
    public void handleCreateOrder(String message) {
        try {
            System.out.println("收到购买订单消息" + message);
            String[] parts = message.split(",");
            Integer buyerId = Integer.parseInt(parts[0]);
            Integer itemId = Integer.parseInt(parts[1]);
            String orderId = parts[2];

            // 1. 验证用户和商品是否存在
            if (!userInfoRepository.existsById(buyerId)) {
                System.err.println("买家不存在: " + buyerId);
                return;
            }

            Item item = itemRepository.findByItemId(itemId);
            if (item == null) {
                System.err.println("商品不存在: " + itemId);
                return;
            }

            if (!userInfoRepository.existsById(item.getSellerId())) {
                System.err.println("卖家不存在: " + item.getSellerId());
                return;
            }

            // 2. 创建订单对象
            Order order = new Order();
            order.setOrderId(orderId);
            order.setItem(item);
            UserInfo buyer = userInfoRepository.findByUserId(buyerId);
            order.setBuyer(buyer);
            UserInfo seller = userInfoRepository.findByUserId(item.getSellerId());
            order.setSeller(seller);
            order.setOrderAmount(item.getPrice());
            order.setOrderStatus(0); // 待确认

            // 3. 保存订单到数据库
            orderDao.save(order);

            // 4. 更新数据库商品状态
            item.setIsAvailable(false);
            itemRepository.save(item);

            // 5. 维护Redis缓存集合
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "update_time");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "price");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "likes");
            
            redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "update_time");
            redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "price");
            redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "likes");

            System.out.println("订单创建成功: " + orderId);
        } catch (Exception e) {
            System.err.println("处理创建订单消息失败: " + e.getMessage());
        }
    }

    // 处理购买商品消息（兼容旧版本）
    @KafkaListener(topics = "buyItem", groupId = "order-group")
    @Transactional
    public void handleBuyItem(String message) {
        try {
            System.out.println("收到购买订单消息: " + message);
            String[] parts = message.split(",");
            Integer buyerId = Integer.parseInt(parts[0]);
            Integer itemId = Integer.parseInt(parts[1]);

            // 更新数据库商品状态
            Item item = itemRepository.findByItemId(itemId);
            if (item != null) {
                item.setIsAvailable(false);
                itemRepository.save(item);

                // 更新Redis缓存
                redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 5, java.util.concurrent.TimeUnit.MINUTES);
                
                // 从可售商品集合中移除
                redisUtils.removeItemFromOnSaleSortedSet(itemId, "update_time");
                redisUtils.removeItemFromOnSaleSortedSet(itemId, "price");
                redisUtils.removeItemFromOnSaleSortedSet(itemId, "likes");
                
                // 从分类可售商品集合中移除
                redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "update_time");
                redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "price");
                redisUtils.removeItemFromCategorySortedSet(item.getCategoryId(), itemId, "likes");
            }
        } catch (Exception e) {
            System.err.println("处理购买商品消息失败: " + e.getMessage());
        }
    }

    // 处理订单确认消息
    @KafkaListener(topics = "OrderConfirm", groupId = "order-group")
    @Transactional
    public void handleOrderConfirm(String orderId) {
        try {
            System.out.println("收到确认订单消息: " + orderId);
            Order order = orderDao.findByOrderId(orderId);
            if (order != null) {
                order.setOrderStatus(1); // 已确认
                order.setConfirmTime(LocalDateTime.now());
                orderDao.save(order);

                // 更新Redis缓存
                redisUtils.cacheOrderDetail(orderId, order);
            }
        } catch (Exception e) {
            System.err.println("处理订单确认消息失败: " + e.getMessage());
        }
    }

    // 处理订单取消消息
    @KafkaListener(topics = "OrderCancel", groupId = "order-group")
    @Transactional
    public void handleOrderCancel(String orderId) {
        try {
            System.out.println("收到取消订单消息: " + orderId);
            Order order = orderDao.findByOrderId(orderId);
            if (order != null) {
                order.setOrderStatus(4); // 已取消
                order.setCancelTime(LocalDateTime.now());
                orderDao.save(order);

                // 恢复商品可售状态
                Item item = order.getItem();
                if (item != null) {
                    item.setIsAvailable(true);
                    itemRepository.save(item);

                    // 更新Redis缓存
                    redisUtils.setCache(redisUtils.generateItemDetailKey(item.getItemId()), item, 5, java.util.concurrent.TimeUnit.MINUTES);
                    
                    // 重新添加到可售商品集合
                    double updateScore = item.getUpdateTime() != null
                        ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                        : System.currentTimeMillis() / 1000.0;
                    double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                    double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
                    
                    redisUtils.addItemToOnSaleSortedSet(item.getItemId(), updateScore, "update_time");
                    redisUtils.addItemToOnSaleSortedSet(item.getItemId(), priceScore, "price");
                    redisUtils.addItemToOnSaleSortedSet(item.getItemId(), likesScore, "likes");
                    
                    redisUtils.addItemToCategorySortedSet(item.getCategoryId(), item.getItemId(), updateScore, "update_time");
                    redisUtils.addItemToCategorySortedSet(item.getCategoryId(), item.getItemId(), priceScore, "price");
                    redisUtils.addItemToCategorySortedSet(item.getCategoryId(), item.getItemId(), likesScore, "likes");
                }

                // 更新Redis缓存
                redisUtils.cacheOrderDetail(orderId, order);
            }
        } catch (Exception e) {
            System.err.println("处理订单取消消息失败: " + e.getMessage());
        }
    }

    // 处理订单完成消息
    @KafkaListener(topics = "OrderComplete", groupId = "order-group")
    @Transactional
    public void handleOrderComplete(String message) {
        try {
            System.out.println("收到确认收货消息: " + message);
            String[] parts = message.split(",");
            String orderId = parts[0];
            String confirmType = parts[1];

            Order order = orderDao.findByOrderId(orderId);
            if (order != null) {
                if ("buyer".equals(confirmType)) {
                    order.setIfBuyerConfirm(1);
                } else if ("seller".equals(confirmType)) {
                    order.setIfSellerConfirm(1);
                }

                // 检查是否双方都已确认
                if (order.getIfBuyerConfirm() == 1 && order.getIfSellerConfirm() == 1) {
                    order.setOrderStatus(3); // 已完成
                    order.setFinishTime(LocalDateTime.now());
                } else {
                    order.setOrderStatus(2); // 等待双方确认
                }

                orderDao.save(order);

                // 更新Redis缓存
                redisUtils.cacheOrderDetail(orderId, order);
            }
        } catch (Exception e) {
            System.err.println("处理订单完成消息失败: " + e.getMessage());
        }
    }

    // 处理订单评价消息
    @KafkaListener(topics = "OrderCredit", groupId = "order-group")
    @Transactional
    public void handleOrderCredit(String message) {
        try {
            System.out.println("收到评论消息: " + message);
            String[] parts = message.split(",");
            String orderId = parts[0];
            String creditType = parts[1];
            Integer credit = Integer.parseInt(parts[2]);

            Order order = orderDao.findByOrderId(orderId);
            if (order != null) {
                if ("buyer".equals(creditType)) {
                    order.setBuyerCredit(credit);
                    
                    // 更新买家信用评分
                    Integer buyerId = order.getBuyer().getUserId();
                    UserInfo buyer = userInfoRepository.findByUserId(buyerId);
                    if (buyer != null) {
                        Integer creditTime = buyer.getCreditTime();
                        BigDecimal credits = buyer.getCreditScore();
                        // 使用BigDecimal进行精确计算
                        BigDecimal creditTimeBD = new BigDecimal(creditTime);
                        BigDecimal creditBD = new BigDecimal(credit);
                        BigDecimal newCreditTimeBD = new BigDecimal(creditTime + 1);
                        
                        credits = credits.multiply(creditTimeBD).add(creditBD).divide(newCreditTimeBD, 2, BigDecimal.ROUND_HALF_UP);
                        buyer.setCreditScore(credits);
                        buyer.setCreditTime(creditTime + 1);
                        userInfoRepository.save(buyer);
                    }
                } else if ("seller".equals(creditType)) {
                    order.setSellerCredit(credit);
                    
                    // 更新卖家信用评分
                    Integer sellerId = order.getSeller().getUserId();
                    UserInfo seller = userInfoRepository.findByUserId(sellerId);
                    if (seller != null) {
                        Integer creditTime = seller.getCreditTime();
                        BigDecimal credits = seller.getCreditScore();
                        // 使用BigDecimal进行精确计算
                        BigDecimal creditTimeBD = new BigDecimal(creditTime);
                        BigDecimal creditBD = new BigDecimal(credit);
                        BigDecimal newCreditTimeBD = new BigDecimal(creditTime + 1);
                        
                        credits = credits.multiply(creditTimeBD).add(creditBD).divide(newCreditTimeBD, 2, BigDecimal.ROUND_HALF_UP);
                        seller.setCreditScore(credits);
                        seller.setCreditTime(creditTime + 1);
                        userInfoRepository.save(seller);
                    }
                }

                orderDao.save(order);

                // 更新Redis缓存
                redisUtils.cacheOrderDetail(orderId, order);
            }
        } catch (Exception e) {
            System.err.println("处理订单评价消息失败: " + e.getMessage());
        }
    }

    // 处理订单缓存预热消息
    @KafkaListener(topics = "orderCachePreheat", groupId = "order-group")
    public void handleOrderCachePreheat(String message) {
        try {
            String[] parts = message.split(",");
            Integer userId = Integer.parseInt(parts[0]);
            String type = parts[1]; // buyer 或 seller
            int page = Integer.parseInt(parts[2]);
            int size = Integer.parseInt(parts[3]);

            System.out.println("开始预热订单缓存: " + type + ", userId: " + userId + ", page: " + page + ", size: " + size);

            // 从数据库查询订单
            UserInfo user = userInfoRepository.findByUserId(userId);
            if (user == null) {
                System.err.println("用户不存在: " + userId);
                return;
            }

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"));

            if ("buyer".equals(type)) {
                org.springframework.data.domain.Page<Order> orderPage = orderDao.findByBuyer(user, pageable);
                for (Order order : orderPage.getContent()) {
                    redisUtils.cacheOrderDetail(order.getOrderId(), order);
                    redisUtils.addOrderToBuyerSet(userId, order.getOrderId(), order);
                }
            } else if ("seller".equals(type)) {
                org.springframework.data.domain.Page<Order> orderPage = orderDao.findBySeller(user, pageable);
                for (Order order : orderPage.getContent()) {
                    redisUtils.cacheOrderDetail(order.getOrderId(), order);
                    redisUtils.addOrderToSellerSet(userId, order.getOrderId(), order);
                }
            }

            System.out.println("订单缓存预热完成: " + type + ", userId: " + userId);
        } catch (Exception e) {
            System.err.println("处理订单缓存预热消息失败: " + e.getMessage());
        }
    }

    // 处理订单详情缓存预热消息
    @KafkaListener(topics = "orderDetailPreheat", groupId = "order-group")
    public void handleOrderDetailPreheat(String orderId) {
        try {
            System.out.println("开始预热订单详情缓存: " + orderId);
            
            Order order = orderDao.findByOrderId(orderId);
            if (order != null) {
                redisUtils.cacheOrderDetail(orderId, order);
                System.out.println("订单详情缓存预热完成: " + orderId);
            } else {
                System.err.println("订单不存在: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("处理订单详情缓存预热消息失败: " + e.getMessage());
        }
    }
}
