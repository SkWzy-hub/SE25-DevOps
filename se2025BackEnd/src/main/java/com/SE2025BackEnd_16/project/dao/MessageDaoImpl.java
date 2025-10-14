package com.SE2025BackEnd_16.project.dao;

import com.SE2025BackEnd_16.project.entity.Message;
import com.SE2025BackEnd_16.project.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 留言数据访问实现类
 */
@Repository
public class MessageDaoImpl implements MessageDao {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Override
    @Transactional
    public Message save(Message message) {
        return messageRepository.save(message);
    }
    
    @Override
    public Optional<Message> findById(Integer messageId) {
        return messageRepository.findById(messageId);
    }
    
    @Override
    public List<Message> findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(Integer itemId, Integer parentId, Boolean isDeleted) {
        return messageRepository.findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(itemId, parentId, isDeleted);
    }
    
    @Override
    public List<Message> findByParentIdAndIsDeletedOrderByReplyTimeAsc(Integer parentId, Boolean isDeleted) {
        return messageRepository.findByParentIdAndIsDeletedOrderByReplyTimeAsc(parentId, isDeleted);
    }
    
    @Override
    public long countByItemIdAndIsDeleted(Integer itemId, Boolean isDeleted) {
        return messageRepository.countByItemIdAndIsDeleted(itemId, isDeleted);
    }
    
    @Override
    @Transactional
    public void softDeleteById(Integer messageId) {
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setIsDeleted(true);
            messageRepository.save(message);
        });
    }
} 