package com.SE2025BackEnd_16.project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfoDto userInfo;
    
    public static LoginResponse success(String token, UserInfoDto userInfo) {
        return new LoginResponse(true, "登录成功", token, userInfo);
    }
    
    public static LoginResponse failure(String message) {
        return new LoginResponse(false, message, null, null);
    }
}
