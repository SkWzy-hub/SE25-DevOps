package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;
    
    @Column(name = "parent_id")
    private Integer parentId = 0;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "reply_time")
    private LocalDateTime replyTime;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "item_id", nullable = false)
    private Integer itemId;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    // 父留言ID已经通过parentId字段处理，不需要对象关联
    
    // 移除replies集合，通过Repository查询获取回复
    
    @PrePersist
    protected void onCreate() {
        replyTime = LocalDateTime.now();
    }
} 