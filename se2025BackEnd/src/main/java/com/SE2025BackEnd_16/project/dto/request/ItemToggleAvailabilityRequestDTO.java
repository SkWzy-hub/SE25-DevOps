package com.SE2025BackEnd_16.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 商品上下架请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemToggleAvailabilityRequestDTO {
    
    @NotNull(message = "可用状态不能为空")
    private Boolean isAvailable;
    
    @NotNull(message = "操作者ID不能为空")
    private Integer operatorId;
    
    private String reason; // 上下架原因，可选
    
    /**
     * 获取操作描述
     */
    public String getOperationDescription() {
        return isAvailable ? "上架商品" : "下架商品";
    }
    
    /**
     * 是否为上架操作
     */
    public boolean isOnlineOperation() {
        return Boolean.TRUE.equals(isAvailable);
    }
    
    /**
     * 是否为下架操作
     */
    public boolean isOfflineOperation() {
        return Boolean.FALSE.equals(isAvailable);
    }
} 