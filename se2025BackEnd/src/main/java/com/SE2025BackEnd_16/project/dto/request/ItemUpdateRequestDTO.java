package com.SE2025BackEnd_16.project.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 商品更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpdateRequestDTO {
    
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题不能超过100个字符")
    private String title;
    
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @DecimalMax(value = "999999.99", message = "商品价格不能超过999999.99")
    private BigDecimal price;
    
    @NotBlank(message = "商品分类不能为空")
    private String category;
    
    @NotBlank(message = "新旧程度不能为空")
    private String condition;
    
    @Size(max = 500, message = "商品描述不能超过500个字符")
    private String description;
    
    // 图片为可选项，更新时可能不需要更换图片
    private MultipartFile image;
    
    @NotNull(message = "卖家ID不能为空")
    private Integer sellerId;
    
    /**
     * 验证是否有新图片上传
     */
    public boolean hasNewImage() {
        return image != null && !image.isEmpty();
    }
} 