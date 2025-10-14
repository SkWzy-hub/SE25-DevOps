package com.SE2025BackEnd_16.project.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户注册响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterResponseDTO {
    
    private Integer userId;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String message;
    
    public static UserRegisterResponseDTO success(Integer userId, String username, String email, String phone, String role) {
        return UserRegisterResponseDTO.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .phone(phone)
                .role(role)
                .message("注册成功")
                .build();
    }
} 