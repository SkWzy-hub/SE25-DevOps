package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.io.Serializable;

@Entity
@Table(name = "user_views_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserViewRecord.UserViewRecordId.class)
public class UserViewRecord {
    
    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Id
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
    
    @Column(name = "view_time")

    private Timestamp viewTime;

    
    @Column(name = "category_view_counts")
    private Integer categoryViewCounts;
    

    // 复合主键类
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserViewRecordId implements Serializable {
        private Integer userId;
        private Integer categoryId;

    }

} 