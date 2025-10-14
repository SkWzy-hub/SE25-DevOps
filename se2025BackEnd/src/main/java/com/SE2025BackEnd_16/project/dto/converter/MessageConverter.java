package com.SE2025BackEnd_16.project.dto.converter;

import com.SE2025BackEnd_16.project.dto.request.MessageCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.MessageResponseDTO;
import com.SE2025BackEnd_16.project.entity.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 留言转换器
 */
@Component
public class MessageConverter {
    
    /**
     * CreateRequestDTO转Entity
     */
    public Message toEntity(MessageCreateRequestDTO requestDTO) {
        Message message = new Message();
        message.setUserId(requestDTO.getUserId());
        message.setItemId(requestDTO.getItemId());
        message.setContent(requestDTO.getContent());
        message.setParentId(requestDTO.getParentId() == null ? 0 : requestDTO.getParentId());
        message.setIsDeleted(false);
        
        return message;
    }
    
    /**
     * Entity转ResponseDTO
     */
    public MessageResponseDTO toResponseDTO(Message message, String username) {
        return MessageResponseDTO.builder()
                .messageId(message.getMessageId())
                .itemId(message.getItemId())
                .content(message.getContent())
                .userId(message.getUserId())
                .username(username)
                .parentId(message.getParentId())

                .createTime(message.getReplyTime())
                .updateTime(message.getReplyTime())
                .build();
    }
    
    /**
     * Entity转ResponseDTO带回复列表
     */
    public MessageResponseDTO toResponseDTO(Message message, String username, List<MessageResponseDTO> replies) {
        return MessageResponseDTO.builder()
                .messageId(message.getMessageId())
                .itemId(message.getItemId())
                .content(message.getContent())
                .userId(message.getUserId())
                .username(username)
                .parentId(message.getParentId())

                .createTime(message.getReplyTime())
                .updateTime(message.getReplyTime())
                .replies(replies)
                .build();
    }
    
    /**
     * Entity列表转ResponseDTO列表
     */
    public List<MessageResponseDTO> toResponseDTOList(List<Message> messages, List<String> usernames) {
        return messages.stream()
                .map(message -> {
                    int index = messages.indexOf(message);
                    String username = index < usernames.size() ? usernames.get(index) : "未知用户";
                    return toResponseDTO(message, username);
                })
                .collect(Collectors.toList());
    }
} 