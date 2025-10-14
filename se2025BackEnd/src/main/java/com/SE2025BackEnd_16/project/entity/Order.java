package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @Column(name = "order_id", length = 32)
    private String orderId;
    
    @Column(name = "seller_credit")
    private Integer sellerCredit;
    
    @Column(name = "buyer_credit")
    private Integer buyerCredit;
    
    @Column(name = "order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderAmount;
    
    @Column(name = "if_buyer_confirm")
    private Integer ifBuyerConfirm = 0;
    
    @Column(name = "if_seller_confirm")
    private Integer ifSellerConfirm = 0;
    
    @Column(name = "order_status")
    private Integer orderStatus = 0; // 0待确认，1已确认，2等待双方确认，3已完成，4已取消
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;
    
    @Column(name = "finish_time")
    private LocalDateTime finishTime;
    
    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "item_id", referencedColumnName = "item_id")
    private Item item;
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "buyer_id", referencedColumnName = "user_id")
    private UserInfo buyer;
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "seller_id", referencedColumnName = "user_id")
    private UserInfo seller;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
} 