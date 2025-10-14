package com.SE2025BackEnd_16.project.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 商品发布请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateRequestDTO {
    
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题不能超过100个字符")
    private String title;
    
    @NotBlank(message = "价格不能为空")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "价格格式不正确，请输入有效的数字（最多2位小数）")
    private String price;
    
    @NotBlank(message = "商品分类不能为空")
    private String category;
    
    @NotBlank(message = "新旧程度不能为空")
    @Size(max = 100, message = "新旧程度不能超过100个字符")
    private String condition;
    
    @Size(max = 5000, message = "商品描述不能超过5000个字符")
    private String description;
    
    @NotNull(message = "需要上传一张商品图片")
    private MultipartFile image;
    
    private Integer sellerId; // 由后端从用户会话中获取
} 