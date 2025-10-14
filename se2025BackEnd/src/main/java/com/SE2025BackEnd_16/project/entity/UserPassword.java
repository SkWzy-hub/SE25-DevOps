package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users_password")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPassword {
    
    @Id
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "password", nullable = false, length = 100)
    private String password;
    
    // 移除对象关联，使用外键字段，避免循环引用
} 