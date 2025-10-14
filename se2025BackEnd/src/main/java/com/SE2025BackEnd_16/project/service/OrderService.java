package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.dao.OrderDao;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.OrderRepository;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dto.response.OrderResponseDTO;
import com.SE2025BackEnd_16.project.dto.converter.OrderConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    
    @Autowired
    private OrderDao orderDao;
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private KafkaUtils kafkaUtils;
    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private OrderConverter orderConverter;

    // ✅ 轻量级操作：获取用户的购买订单（缓存优先，数据库兜底）
    public Page<Order> getUserBuyOrders(Integer userId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        
        // 1. 从Redis获取买家订单ID列表
        List<String> orderIds = redisUtils.getBuyerOrderIds(userId, page, size);
        
        List<Order> orders;
        long total;
        
        if (!orderIds.isEmpty()) {
            // Redis命中，批量获取订单详情
            System.out.println("Redis命中买家订单，获取到" + orderIds.size() + "个订单ID");
            orders = redisUtils.getOrdersByIds(orderIds);
            total = redisUtils.getBuyerOrderCount(userId);
        } else {
            // Redis未命中，从数据库查询并立即返回，同时异步补缓存
            System.out.println("Redis未命中买家订单，从数据库查询并异步补缓存");
            UserInfo user = userInfoRepository.findByUserId(userId);
            Page<Order> orderPage = orderDao.findByBuyer(user, pageable);
            orders = orderPage.getContent();
            total = orderPage.getTotalElements();
            
            // 立即补缓存当前页数据
            for (Order order : orders) {
                redisUtils.cacheOrderDetail(order.getOrderId(), order);
                redisUtils.addOrderToBuyerSet(userId, order.getOrderId(), order);
            }
            
            // 发送Kafka消息异步补缓存其他页数据
            kafkaUtils.sendMessage("orderCachePreheat", userId + ",buyer," + page + "," + size);
        }
        
        return new org.springframework.data.domain.PageImpl<>(orders, pageable, total);
    }

    
    // ✅ 轻量级操作：获取用户的销售订单（缓存优先，数据库兜底）
    public Page<Order> getUserSellOrders(Integer userId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        
        // 1. 从Redis获取卖家订单ID列表
        List<String> orderIds = redisUtils.getSellerOrderIds(userId, page, size);
        
        List<Order> orders;
        long total;
        
        if (!orderIds.isEmpty()) {
            // Redis命中，批量获取订单详情
            System.out.println("Redis命中卖家订单，获取到" + orderIds.size() + "个订单ID");
            orders = redisUtils.getOrdersByIds(orderIds);
            total = redisUtils.getSellerOrderCount(userId);
        } else {
            // Redis未命中，从数据库查询并立即返回，同时异步补缓存
            System.out.println("Redis未命中卖家订单，从数据库查询并异步补缓存");
            UserInfo user = userInfoRepository.findByUserId(userId);
            Page<Order> orderPage = orderDao.findBySeller(user, pageable);
            orders = orderPage.getContent();
            total = orderPage.getTotalElements();
            
            // 立即补缓存当前页数据
            for (Order order : orders) {
                redisUtils.cacheOrderDetail(order.getOrderId(), order);
                redisUtils.addOrderToSellerSet(userId, order.getOrderId(), order);
            }
            
            // 发送Kafka消息异步补缓存其他页数据
            kafkaUtils.sendMessage("orderCachePreheat", userId + ",seller," + page + "," + size);
        }
        
        return new org.springframework.data.domain.PageImpl<>(orders, pageable, total);
    }
    
    // 🔍 创建订单（高并发安全，纯缓存操作，数据库操作异步）
    public boolean createOrder(Integer buyerId, Integer itemId) {
        // 1. 买家不能购买自己的商品（先检查，避免不必要的锁竞争）
        Item item = redisUtils.getItemDetail(itemId);
        if (item == null) {
            throw new RuntimeException("商品不存在或缓存未命中");
        }
        
        if (buyerId.equals(item.getSellerId())) {
            throw new RuntimeException("不能购买自己的商品");
        }
        
        // 2. 原子性地检查和预留商品（高并发安全）
        if (!redisUtils.atomicCheckAndReserveItem(itemId, buyerId)) {
            throw new RuntimeException("商品已被其他用户购买或已下架");
        }
        
        // 3. 重新获取最新的商品信息（因为状态已经更新）
        item = redisUtils.getItemDetail(itemId);
        
        // 4. 创建订单对象（纯内存操作）
        Order order = new Order();
        order.setOrderId(generateOrderId());
        order.setItem(item);
        order.setOrderAmount(item.getPrice());
        order.setOrderStatus(0); // 待确认
        order.setCreateTime(LocalDateTime.now()); // 新增这一行


        UserInfo buyer = userInfoRepository.findByUserId(buyerId);
        order.setBuyer(buyer);
        UserInfo seller = userInfoRepository.findByUserId(item.getSellerId());
        order.setSeller(seller);


        
        // 5. 立即缓存订单详情和订单集合
        redisUtils.cacheOrderDetail(order.getOrderId(), order);
        redisUtils.addOrderToBuyerSet(buyerId, order.getOrderId(), order);
        redisUtils.addOrderToSellerSet(item.getSellerId(), order.getOrderId(), order);
        
        // 6. 发送Kafka消息异步处理数据库操作
        kafkaUtils.sendMessage("createOrder", buyerId + "," + itemId + "," + order.getOrderId());
        
        return true;
    }
    
    // 🔍 卖家确认订单（缓存优先，数据库兜底）
    public void confirmOrder(String orderId) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        
        if (order.getOrderStatus() == 4) {
            throw new RuntimeException("订单已取消");
        }
        
        // 2. 立即更新缓存中的订单状态
        order.setOrderStatus(1); // 已确认
        order.setConfirmTime(LocalDateTime.now());
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderConfirm", orderId);
    }
    
    // 🔍 获取订单详情（缓存优先，数据库兜底）
    public Order getOrderDetail(String orderId) {
        // 1. 从Redis缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 2. Redis未命中，从数据库查询并立即返回，同时异步补缓存
            System.out.println("订单详情缓存未命中，从数据库查询并异步补缓存: " + orderId);
            order = orderDao.findByOrderId(orderId);
            if (order != null) {
                // 立即补缓存
                redisUtils.cacheOrderDetail(orderId, order);
                // 发送Kafka消息异步补缓存其他相关数据
                kafkaUtils.sendMessage("orderDetailPreheat", orderId);
            }
        }
        return order;
    }

    // 🔍 取消订单（缓存优先，数据库兜底）
    public void cancelOrder(String orderId) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        
        // 2. 立即更新缓存中的订单状态
        order.setOrderStatus(4); // 已取消
        order.setCancelTime(LocalDateTime.now());
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 恢复商品可售状态
        Item item = order.getItem();
        if (item != null) {
            item.setIsAvailable(true);
            redisUtils.setCache(redisUtils.generateItemDetailKey(item.getItemId()), item, 5, java.util.concurrent.TimeUnit.MINUTES);
        }
        
        // 4. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderCancel", orderId);
    }

    // 🔍 买家确认订单（缓存优先，数据库兜底）
    public void buyerConfirmOrder(String orderId) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        if (order.getOrderStatus() == 4) {
            throw new RuntimeException("订单已取消");
        }
        
        // 2. 立即更新缓存中的订单状态
        order.setIfBuyerConfirm(1);
        if (order.getIfSellerConfirm() == 1) {
            order.setOrderStatus(3); // 已完成
            order.setFinishTime(LocalDateTime.now());
        } else {
            order.setOrderStatus(2); // 等待双方确认
        }
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderComplete", orderId + "," + "buyer");
    }

    // 🔍 卖家确认订单（缓存优先，数据库兜底）
    public void sellerConfirmOrder(String orderId) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        if (order.getOrderStatus() == 4) {
            throw new RuntimeException("订单已取消");
        }
        
        // 2. 立即更新缓存中的订单状态
        order.setIfSellerConfirm(1);
        if (order.getIfBuyerConfirm() == 1) {
            order.setOrderStatus(3); // 已完成
            order.setFinishTime(LocalDateTime.now());
        } else {
            order.setOrderStatus(2); // 等待双方确认
        }
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderComplete", orderId + "," + "seller");
    }

    // 🔍 买家评价（缓存优先，数据库兜底）
    public void buyerCredit(String orderId, Integer credit) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        if (order.getOrderStatus() != 3) {
            throw new RuntimeException("订单还未完成");
        }
        
        // 2. 立即更新缓存中的订单评价
        order.setBuyerCredit(credit);
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderCredit", orderId + "," + "buyer" + "," + credit);
    }
    // 🔍 卖家评价（缓存优先，数据库兜底）
    public void sellerCredit(String orderId, Integer credit) {
        // 1. 从缓存获取订单
        Order order = redisUtils.getOrderDetail(orderId);
        if (order == null) {
            // 缓存未命中，从数据库查询
            order = orderDao.findByOrderId(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }
            // 立即补缓存
            redisUtils.cacheOrderDetail(orderId, order);
        }
        if (order.getOrderStatus() != 3) {
            throw new RuntimeException("订单未完成");
        }
        
        // 2. 立即更新缓存中的订单评价
        order.setSellerCredit(credit);
        redisUtils.cacheOrderDetail(orderId, order);
        
        // 3. 发送Kafka消息异步更新数据库
        kafkaUtils.sendMessage("OrderCredit", orderId + "," + "seller" + "," + credit);
    }
    
    // 生成订单ID
    public String generateOrderId() {
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 1000);
        return String.format("ORD%d%03d", timestamp, random);
    }

    /**
     * 预热订单缓存（系统启动时调用）
     */
    public void preheatOrderCache() {
        try {
            System.out.println("开始预热订单缓存...");
            
            // 获取所有订单
            List<Order> allOrders = orderDao.findAll();
            
            for (Order order : allOrders) {
                // 缓存订单详情
                redisUtils.cacheOrderDetail(order.getOrderId(), order);
                
                // 添加到买家订单集合
                if (order.getBuyer() != null) {
                    redisUtils.addOrderToBuyerSet(order.getBuyer().getUserId(), order.getOrderId(), order);
                }
                
                // 添加到卖家订单集合
                if (order.getSeller() != null) {
                    redisUtils.addOrderToSellerSet(order.getSeller().getUserId(), order.getOrderId(), order);
                }
            }
            
            System.out.println("订单缓存预热完成，共处理" + allOrders.size() + "个订单");
        } catch (Exception e) {
            System.err.println("订单缓存预热失败: " + e.getMessage());
        }
    }


    

} 


