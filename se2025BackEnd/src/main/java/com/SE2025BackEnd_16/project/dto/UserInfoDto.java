package com.SE2025BackEnd_16.project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一的用户信息DTO
 * 合并了UserProfileDto和UserLoginDto的功能
 * 用于Redis缓存和API响应，不包含敏感信息如密码
 * 支持变更追踪，记录哪些字段在缓存中被修改
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {
    private Integer userId;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String note;
    private String role;
    private BigDecimal creditScore;
    private Integer dealTime;
    private Integer creditTime;
    private Boolean status;
    
    // 变更追踪字段 - 记录哪些字段在缓存中被修改了
    @Builder.Default
    private Set<String> modifiedFields = new HashSet<>();
    
    // ================================
    // 变更追踪相关方法
    // ================================
    
    /**
     * 标记字段为已修改
     */
    public void markFieldAsModified(String fieldName) {
        if (modifiedFields == null) {
            modifiedFields = new HashSet<>();
        }
        modifiedFields.add(fieldName);
    }
    
    /**
     * 检查字段是否被修改
     */
    public boolean isFieldModified(String fieldName) {
        return modifiedFields != null && modifiedFields.contains(fieldName);
    }
    
    /**
     * 清除所有修改标记
     */
    public void clearModifications() {
        if (modifiedFields != null) {
            modifiedFields.clear();
        }
    }
    
    /**
     * 检查是否有任何修改
     */
    public boolean hasModifications() {
        return modifiedFields != null && !modifiedFields.isEmpty();
    }
    
    // ================================
    // 增强的setter方法，自动标记修改
    // ================================
    
    public void setUsernameAndMark(String username) {
        this.username = username;
        markFieldAsModified("username");
    }
    
    public void setPhoneAndMark(String phone) {
        this.phone = phone;
        markFieldAsModified("phone");
    }
    
    public void setNoteAndMark(String note) {
        this.note = note;
        markFieldAsModified("note");
    }
    
    public void setAvatarAndMark(String avatar) {
        this.avatar = avatar;
        markFieldAsModified("avatar");
    }
    
    public void setEmailAndMark(String email) {
        this.email = email;
        markFieldAsModified("email");
    }
    
    public void setCreditScoreAndMark(BigDecimal creditScore) {
        this.creditScore = creditScore;
        markFieldAsModified("creditScore");
    }
    
    public void setDealTimeAndMark(Integer dealTime) {
        this.dealTime = dealTime;
        markFieldAsModified("dealTime");
    }
    
    public void setCreditTimeAndMark(Integer creditTime) {
        this.creditTime = creditTime;
        markFieldAsModified("creditTime");
    }
    
    public void setStatusAndMark(Boolean status) {
        this.status = status;
        markFieldAsModified("status");
    }
    
    public void setRoleAndMark(String role) {
        this.role = role;
        markFieldAsModified("role");
    }
} 