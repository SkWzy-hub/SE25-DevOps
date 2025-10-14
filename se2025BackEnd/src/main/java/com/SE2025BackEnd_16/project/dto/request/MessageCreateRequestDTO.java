package com.SE2025BackEnd_16.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 留言创建请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequestDTO {
    
    @NotNull(message = "商品ID不能为空")
    private Integer itemId;
    
    @NotBlank(message = "留言内容不能为空")
    @Size(max = 1000, message = "留言内容不能超过1000个字符")
    private String content;
    
    private Integer parentId; // 父留言ID，为空表示根留言
    
    private Integer userId; // 用户ID，从认证信息中获取
} 