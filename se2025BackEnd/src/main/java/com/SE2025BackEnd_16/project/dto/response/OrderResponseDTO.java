package com.SE2025BackEnd_16.project.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    
    private String orderId;
    private Integer sellerCredit;
    private Integer buyerCredit;
    private BigDecimal orderAmount;
    private Integer ifBuyerConfirm;
    private Integer ifSellerConfirm;
    private Integer orderStatus; // 0待确认，1已确认，2等待双方确认，3已完成，4已取消
    private LocalDateTime createTime;
    private LocalDateTime confirmTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    
    // 商品信息
    private Integer itemId;
    private String itemName;
    private String itemImage;
    private BigDecimal itemPrice;
    
    // 买家信息
    private Integer buyerId;
    private String buyerUsername;
    
    // 卖家信息
    private Integer sellerId;
    private String sellerUsername;
} 