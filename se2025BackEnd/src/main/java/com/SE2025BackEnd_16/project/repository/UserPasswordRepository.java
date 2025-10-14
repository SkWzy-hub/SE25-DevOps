package com.SE2025BackEnd_16.project.repository;

import com.SE2025BackEnd_16.project.entity.UserPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserPasswordRepository extends JpaRepository<UserPassword, Integer> {
    
    // 根据用户ID查找密码信息
    Optional<UserPassword> findByUserId(Integer userId);
    
    // 检查用户是否已设置密码
    boolean existsByUserId(Integer userId);
    
    // 更新用户密码
    @Modifying
    @Transactional
    @Query("UPDATE UserPassword up SET up.password = :newPassword WHERE up.userId = :userId")
    int updatePasswordByUserId(@Param("userId") Integer userId, @Param("newPassword") String newPassword);
    
    // 删除用户密码信息
    void deleteByUserId(Integer userId);
}
