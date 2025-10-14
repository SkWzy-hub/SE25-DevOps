package com.SE2025BackEnd_16.project.Controller;

import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.utils.JwtAuthHelper;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private KafkaUtils kafkaUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private JwtAuthHelper jwtAuthHelper;

    @GetMapping("/purchased/{page}/{size}")
    public ResponseEntity<?> purchased(@PathVariable int page, @PathVariable int size, HttpServletRequest request) {
        // 从JWT token中获取当前用户ID
        Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
        if (currentUserId == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("message", "用户未登录或token无效");
            return ResponseEntity.badRequest().body(errorMap);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orders = orderService.getUserBuyOrders(currentUserId, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/sold/{page}/{size}")
    public ResponseEntity<?> sold(@PathVariable int page, @PathVariable int size, HttpServletRequest request) {
        // 从JWT token中获取当前用户ID
        Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
        if (currentUserId == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("message", "用户未登录或token无效");
            return ResponseEntity.badRequest().body(errorMap);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orders = orderService.getUserSellOrders(currentUserId, pageable);
//        kafkaUtils.sendMessage("orderCache", currentUserId + "," + "seller" + "," + page + ", " + size);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/buy/{productId}")
    public ResponseEntity<?> buy(@PathVariable int productId, HttpServletRequest request) {
        // 从JWT token中获取当前用户ID
        Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
        if (currentUserId == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("success", false);
            errorMap.put("message", "用户未登录或token无效");
            return ResponseEntity.badRequest().body(errorMap);
        }

        Map<String, Object> map = new HashMap<>();

        try{
            orderService.createOrder(currentUserId, productId);
            map.put("success", true);
            return ResponseEntity.ok(map);
        }
        catch(Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }
    }

    @PostMapping
    public ResponseEntity<?> getOrderDetail(@RequestBody Map<String, Object> map){
//        System.out.println("000");
        String orderId = (String) map.get("orderId");
//
        try{
            Order order = orderService.getOrderDetail(orderId);
//            kafkaUtils.sendMessage("orderDetailCache", orderId);
            return ResponseEntity.ok(order);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }

    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Map<String, Object> map1 = new HashMap<>();
        // 无论缓存是否命中，都发Kafka消息
//        kafkaUtils.sendMessage("orderConfirm", orderId);
        try{
            orderService.confirmOrder(orderId);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }

    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Map<String, Object> map1 = new HashMap<>();
//        kafkaUtils.sendMessage("orderCancel", orderId);
        try{
            orderService.cancelOrder(orderId);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }

    }

    @PostMapping("/buyer/complete")
    public ResponseEntity<?> buyerConfirmOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Map<String, Object> map1 = new HashMap<>();
//        kafkaUtils.sendMessage("orderComplete", orderId + "," + "buyer");
        try{
            orderService.buyerConfirmOrder(orderId);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }


    }

    @PostMapping("/seller/complete")
    public ResponseEntity<?> sellerConfirmOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Map<String, Object> map1 = new HashMap<>();
//        kafkaUtils.sendMessage("orderComplete", orderId + "," + "seller");
        try{
            orderService.sellerConfirmOrder(orderId);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }



    }

    @PostMapping("/buyer/credit")
    public ResponseEntity<?> buyerCreditOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Integer credit = (Integer) map.get("credit");
        Map<String, Object> map1 = new HashMap<>();
//        kafkaUtils.sendMessage("orderCredit", orderId + "," + "buyer" + "," + credit);
        try{
            orderService.buyerCredit(orderId, credit);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }

    }

    @PostMapping("/seller/credit")
    public ResponseEntity<?> sellerCreditOrder(@RequestBody Map<String, Object> map){
        String orderId = (String) map.get("orderId");
        Integer credit = (Integer) map.get("credit");
        Map<String, Object> map1 = new HashMap<>();
//        kafkaUtils.sendMessage("orderCredit", orderId + "," + "seller" + "," + credit);
        try{
            orderService.sellerCredit(orderId, credit);
            map1.put("success", true);
            return ResponseEntity.ok(map1);
        }
        catch (Exception e){
            map.put("success", false);
            map.put("message", e.getMessage());
            return ResponseEntity.ok(map);
        }
    }
}