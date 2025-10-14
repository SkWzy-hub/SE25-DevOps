package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    
    // 获取商品的所有根留言（parentId = 0）
    List<Message> findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(Integer itemId, Integer parentId, Boolean isDeleted);
    
    // 获取某条留言的所有回复
    List<Message> findByParentIdAndIsDeletedOrderByReplyTimeAsc(Integer parentId, Boolean isDeleted);
    
    // 获取用户的所有留言
    List<Message> findByUserIdAndIsDeletedOrderByReplyTimeDesc(Integer userId, Boolean isDeleted);
    
    // 获取商品的所有留言数量
    long countByItemIdAndIsDeleted(Integer itemId, Boolean isDeleted);
} 