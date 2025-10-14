package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dao.MessageDao;
import com.SE2025BackEnd_16.project.dto.converter.MessageConverter;
import com.SE2025BackEnd_16.project.dto.request.MessageCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.MessageResponseDTO;
import com.SE2025BackEnd_16.project.entity.Message;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 留言服务实现类
 */
@Slf4j
@Service
@Transactional
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private MessageDao messageDao;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private MessageConverter messageConverter;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private KafkaUtils kafkaUtils;

    @Override
    public MessageResponseDTO addMessage(MessageCreateRequestDTO requestDTO) {
//        log.info("添加留言: 用户ID={}, 商品ID={}, 父留言ID={}",
//                requestDTO.getUserId(), requestDTO.getItemId(), requestDTO.getParentId());
        
        // 1. 验证用户是否存在
        if (!userInfoRepository.existsById(requestDTO.getUserId())) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 如果有父留言，验证父留言是否存在
        if (requestDTO.getParentId() != null && requestDTO.getParentId() != 0) {
            if (!messageDao.findById(requestDTO.getParentId()).isPresent()) {
                throw new RuntimeException("父留言不存在");
            }
        }
        
        // 3. DTO转Entity
        Message message = messageConverter.toEntity(requestDTO);
        
        // 4. 保存留言
        Message savedMessage = messageDao.save(message);
        // 发送Kafka消息，异步缓存新留言
        String messageInfo = savedMessage.getMessageId() +"," + requestDTO.getItemId();
        kafkaUtils.sendMessage("addMessage", messageInfo);
//        log.info("留言添加成功，ID: {}", savedMessage.getMessageId());
        
        // 5. 获取用户信息并转换为ResponseDTO
        String username = userInfoRepository.findById(savedMessage.getUserId())
                .map(UserInfo::getUsername)
                .orElse("未知用户");
        
        return messageConverter.toResponseDTO(savedMessage, username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getRootMessages(Integer itemId) {
//        log.info("获取商品根留言，商品ID: {}", itemId);
        
        // 1. 获取根留言（parentId = 0）
        List<Message> rootMessages = messageDao.findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(itemId, 0, false);
        
        // 2. 转换为ResponseDTO并获取回复
        return rootMessages.stream()
                .map(message -> {
                    String username = userInfoRepository.findById(message.getUserId())
                            .map(UserInfo::getUsername)
                            .orElse("未知用户");
                    
                    // 获取回复列表
                    List<MessageResponseDTO> replies = getMessageReplies(message.getMessageId());
                    
                    return messageConverter.toResponseDTO(message, username, replies);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessageReplies(Integer parentId) {
//        log.info("获取留言回复，父留言ID: {}", parentId);
        // 1. 获取回复列表
        List<Message> replies = redisUtils.getReplyMessages(parentId);
        if (replies == null || replies.isEmpty()) {
            replies = messageDao.findByParentIdAndIsDeletedOrderByReplyTimeAsc(parentId, false);
            if (replies != null && !replies.isEmpty()) {
                System.out.println("缓存未命中ReplyMessage：" + parentId);
                kafkaUtils.sendMessage("replyMessage", String.valueOf(parentId));
            }
        }
        // 2. 转换为ResponseDTO
        return replies.stream()
                .map(reply -> {
                    String username = userInfoRepository.findById(reply.getUserId())
                            .map(UserInfo::getUsername)
                            .orElse("未知用户");
                    return messageConverter.toResponseDTO(reply, username);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteMessage(Integer messageId) {
//        log.info("删除留言，留言ID: {}", messageId);
        
        // 1. 验证留言是否存在
        if (!messageDao.findById(messageId).isPresent()) {
            throw new RuntimeException("留言不存在");
        }
        
        // 2. 软删除留言
        messageDao.softDeleteById(messageId);
        kafkaUtils.sendMessage("deleteMessage", String.valueOf(messageId));
//        log.info("留言删除成功，ID: {}", messageId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getMessageCount(Integer itemId) {
//        log.info("获取商品留言数量，商品ID: {}", itemId);
        return messageDao.countByItemIdAndIsDeleted(itemId, false);
    }
} 