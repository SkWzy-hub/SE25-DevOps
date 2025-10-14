package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.dto.request.MessageCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.MessageResponseDTO;

import java.util.List;

/**
 * 留言服务接口
 */
public interface MessageService {
    
    /**
     * 添加留言
     */
    MessageResponseDTO addMessage(MessageCreateRequestDTO requestDTO);
    
    /**
     * 获取商品的所有根留言
     */
    List<MessageResponseDTO> getRootMessages(Integer itemId);
    
    /**
     * 获取留言的所有回复
     */
    List<MessageResponseDTO> getMessageReplies(Integer parentId);
    
    /**
     * 删除留言（软删除）
     */
    void deleteMessage(Integer messageId);
    
    /**
     * 获取商品的留言数量
     */
    long getMessageCount(Integer itemId);
} 