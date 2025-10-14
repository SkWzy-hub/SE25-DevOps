package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
    
    // 根据邮箱查找用户
    Optional<UserInfo> findByEmail(String email);
    
    // 根据用户名查找用户
    Optional<UserInfo> findByUsername(String username);
    
    // 根据电话查找用户
    Optional<UserInfo> findByPhone(String phone);


    UserInfo findByUserId(Integer userId);


    
   @Modifying
    @Transactional
    @Query("UPDATE UserInfo u SET u.username = :username WHERE u.userId = :userId")
    int updateUsername(@Param("userId") Integer userId, @Param("username") String username);
    
    // 更新用户电话号码
    @Modifying
    @Transactional
    @Query("UPDATE UserInfo u SET u.phone = :phone WHERE u.userId = :userId")
    int updatePhone(@Param("userId") Integer userId, @Param("phone") String phone);
    
    // 更新用户头像
    @Modifying
    @Transactional
    @Query("UPDATE UserInfo u SET u.avatar = :avatar WHERE u.userId = :userId")
    int updateAvatar(@Param("userId") Integer userId, @Param("avatar") String avatar);
    
    // 更新用户基本信息（用户名、电话、个人签名）
    @Modifying
    @Transactional
    @Query("UPDATE UserInfo u SET u.username = :username, u.phone = :phone, u.note = :note WHERE u.userId = :userId")
    int updateBasicInfo(@Param("userId") Integer userId, 
                       @Param("username") String username,
                       @Param("phone") String phone,
                       @Param("note") String note);
                       
    // 检查邮箱是否已被其他用户使用
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserInfo u WHERE u.email = :email AND u.userId <> :userId")
    boolean existsByEmailAndUserIdNot(@Param("email") String email, @Param("userId") Integer userId);
}

