package com.SE2025BackEnd_16.project.dao;

import com.SE2025BackEnd_16.project.entity.Message;
import java.util.List;
import java.util.Optional;

/**
 * 留言数据访问接口
 */
public interface MessageDao {
    
    /**
     * 保存留言
     */
    Message save(Message message);
    
    /**
     * 根据ID查找留言
     */
    Optional<Message> findById(Integer messageId);
    
    /**
     * 根据商品ID和父留言ID查找留言（根留言）
     */
    List<Message> findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(Integer itemId, Integer parentId, Boolean isDeleted);
    
    /**
     * 根据父留言ID查找回复
     */
    List<Message> findByParentIdAndIsDeletedOrderByReplyTimeAsc(Integer parentId, Boolean isDeleted);
    
    /**
     * 统计商品的留言数量
     */
    long countByItemIdAndIsDeleted(Integer itemId, Boolean isDeleted);
    
    /**
     * 软删除留言
     */
    void softDeleteById(Integer messageId);
} 