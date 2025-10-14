package com.SE2025BackEnd_16.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 商品删除请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDeleteRequestDTO {
    
    @NotNull(message = "操作者ID不能为空")
    private Integer operatorId;
    
    private Boolean forceDelete = false; // 是否强制删除，默认为false
    
    /**
     * 是否为强制删除操作
     */
    public boolean isForceDelete() {
        return Boolean.TRUE.equals(forceDelete);
    }
    
    /**
     * 验证删除权限（业务逻辑方法）
     */
    public boolean validateDeletePermission(Integer itemSellerId) {
        // 只有商品所有者才能删除
        return operatorId.equals(itemSellerId);
    }
} 