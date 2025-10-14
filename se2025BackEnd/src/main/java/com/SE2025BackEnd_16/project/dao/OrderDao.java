package com.SE2025BackEnd_16.project.dao;


import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderDao {
    Page<Order> findByBuyer(UserInfo buyer, Pageable pageable);

    // 通过卖家ID查询订单
    Page<Order> findBySeller(UserInfo seller, Pageable pageable);

    // 通过物品ID查询订单（二手交易平台：每个订单只有一个物品）
    List<Order> findByItem(Item item);

    // 根据订单状态查询
    List<Order> findByOrderStatus(Integer orderStatus);

    Order findByOrderId(String orderId);

    Order save(Order order);
    
    List<Order> findAll();
}
