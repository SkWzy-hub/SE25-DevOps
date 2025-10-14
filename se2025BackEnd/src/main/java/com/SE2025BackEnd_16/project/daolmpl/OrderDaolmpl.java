package com.SE2025BackEnd_16.project.daolmpl;


import com.SE2025BackEnd_16.project.dao.OrderDao;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderDaolmpl implements OrderDao {
    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Page<Order> findByBuyer(UserInfo buyer, Pageable pageable){
        return orderRepository.findByBuyer(buyer, pageable);
    }

    // 通过卖家ID查询订单
    @Override
    public Page<Order> findBySeller(UserInfo seller, Pageable pageable){
        return orderRepository.findBySeller(seller, pageable);
    }

    // 通过物品ID查询订单（二手交易平台：每个订单只有一个物品）
    @Override
    public List<Order> findByItem(Item item){
        return orderRepository.findByItem(item);
    }

    // 根据订单状态查询
    @Override
    public List<Order> findByOrderStatus(Integer orderStatus){
        return orderRepository.findByOrderStatus(orderStatus);
    }

    @Override
    public Order findByOrderId(String orderId){
        return orderRepository.findByOrderId(orderId);
    }

    @Override
    public Order save(Order order){
        return orderRepository.save(order);
    }

    @Override
    public List<Order> findAll(){
        return orderRepository.findAll();
    }
}
