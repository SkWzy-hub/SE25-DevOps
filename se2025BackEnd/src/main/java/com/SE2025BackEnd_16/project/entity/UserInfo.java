package com.SE2025BackEnd_16.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "users_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    
    @Column(name = "email", unique = true, length = 100)
    private String email;
    
    @Column(name = "avatar", length = 200)
    private String avatar;
    
    @Column(name = "note", length = 200)
    private String note = "这个人很懒，什么都没有留下";
    
    @Column(name = "credit_score")
    private BigDecimal creditScore = new BigDecimal("5.00");
    

    @Column(name = "credit_time")
    private Integer creditTime = 0;
    
    @Column(name = "deal_time")
    private Integer dealTime = 0;
    
    @Column(name = "status")
    private Boolean status = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.user;
    
    // 移除双向关联，避免映射冲突，通过Repository查询获取密码
    
    public enum Role {
        user, admin
    }
} 