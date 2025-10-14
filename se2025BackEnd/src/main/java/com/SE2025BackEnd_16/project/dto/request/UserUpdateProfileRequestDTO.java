package com.SE2025BackEnd_16.project.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户更新基本信息请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateProfileRequestDTO {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20个字符之间")
    private String username;
    
    /**
     * 电话号码
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String phone;
    
    /**
     * 个人签名
     */
    @Size(max = 100, message = "个人签名不能超过100个字符")
    private String note;
} 