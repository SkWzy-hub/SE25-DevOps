package com.SE2025BackEnd_16.project.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Size;

/**
 * 更新头像请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvatarRequest {
    
    @Size(max = 200, message = "头像URL长度不能超过200个字符")
    private String avatarUrl;
}
