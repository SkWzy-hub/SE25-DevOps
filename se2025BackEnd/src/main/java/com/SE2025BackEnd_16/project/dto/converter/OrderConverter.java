package com.SE2025BackEnd_16.project.dto.converter;

import com.SE2025BackEnd_16.project.entity.Order;
import com.SE2025BackEnd_16.project.dto.response.OrderResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderConverter {
    
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }
        
        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .sellerCredit(order.getSellerCredit())
                .buyerCredit(order.getBuyerCredit())
                .orderAmount(order.getOrderAmount())
                .ifBuyerConfirm(order.getIfBuyerConfirm())
                .ifSellerConfirm(order.getIfSellerConfirm())
                .orderStatus(order.getOrderStatus())
                .createTime(order.getCreateTime())
                .confirmTime(order.getConfirmTime())
                .finishTime(order.getFinishTime())
                .cancelTime(order.getCancelTime())
                .itemId(order.getItem() != null ? order.getItem().getItemId() : null)
                .itemName(order.getItem() != null ? order.getItem().getItemName() : null)
                .itemImage(order.getItem() != null ? order.getItem().getImageUrl() : null)
                .itemPrice(order.getItem() != null ? order.getItem().getPrice() : null)
                .buyerId(order.getBuyer() != null ? order.getBuyer().getUserId() : null)
                .buyerUsername(order.getBuyer() != null ? order.getBuyer().getUsername() : null)
                .sellerId(order.getSeller() != null ? order.getSeller().getUserId() : null)
                .sellerUsername(order.getSeller() != null ? order.getSeller().getUsername() : null)
                .build();
    }
    
    public List<OrderResponseDTO> toResponseDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
} 