package com.SE2025BackEnd_16.project.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 留言响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDTO {
    
    private Integer messageId;
    private Integer itemId;
    private String content;
    private Integer userId;
    private String username;
    private Integer parentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<MessageResponseDTO> replies; // 子留言列表
} 