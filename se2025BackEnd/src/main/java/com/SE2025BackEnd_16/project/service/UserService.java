package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.entity.UserPassword;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.repository.UserPasswordRepository;
import com.SE2025BackEnd_16.project.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private UserPasswordRepository userPasswordRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RedisService redisService;

    // ================================
    // 用户注册相关方法
    // ================================

    /**
     * 用户注册
     */
    @Transactional
    public UserInfo registerUser(String username, String email, String phone, String password) {
        // 1. 检查邮箱是否已存在
        if (userInfoRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 2. 检查手机号是否已存在
        if (userInfoRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("手机号已被注册");
        }
        
        // 3. 创建用户基本信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setEmail(email);
        userInfo.setPhone(phone);
        userInfo.setNote("这个人很懒，什么都没有留下");
        userInfo.setCreditScore(new BigDecimal("5.00"));
        userInfo.setDealTime(0);
        userInfo.setCreditTime(0);
        userInfo.setStatus(true);
        userInfo.setRole(UserInfo.Role.user);
        
        // 4. 保存用户基本信息
        UserInfo savedUserInfo = userInfoRepository.save(userInfo);
        
        // 5. 创建用户密码
        createUserPassword(savedUserInfo.getUserId(), password);
        
        return savedUserInfo;
    }
    
    /**
     * 检查邮箱是否已被注册
     */
    public boolean isEmailRegistered(String email) {
        return userInfoRepository.findByEmail(email).isPresent();
    }
    
    /**
     * 检查手机号是否已被注册
     */
    public boolean isPhoneRegistered(String phone) {
        return userInfoRepository.findByPhone(phone).isPresent();
    }

    // ================================
    // 用户个人信息修改方法
    // ================================

    /**
     * 更新用户基本信息（用户名 + 电话+个人签名）
     * 采用延迟写入策略：先更新缓存，用户退出时再写入数据库
     */
    @Transactional
    public UpdateResult updateUserBasicInfo(Integer userId, String username, String phone, String note) {
        try {
            // 检查用户是否存在
            if (!userInfoRepository.existsById(userId)) {
                return UpdateResult.failure("用户不存在");
            }
            
            // 延迟写入策略：先更新Redis缓存
            redisService.updateUserInfo(userId, username, phone, note);
            
            // 获取更新后的缓存信息构建返回结果
            UserInfoDto cachedUser = redisService.getUserLoginInfo(userId);
            if (cachedUser != null) {
                System.out.println("用户 " + userId + " 基本信息已更新到缓存，等待退出时写入数据库");
                return UpdateResult.success("用户信息更新成功（缓存中）", cachedUser);
            } else {
                return UpdateResult.failure("缓存更新失败");
            }
            
        } catch (Exception e) {
            return UpdateResult.failure("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 清除用户相关的Redis缓存
     */
    private void clearUserCache(Integer userId, String email) {
        try {
            // 清除用户详细信息缓存
            redisService.removeUserLoginInfo(userId);
            
            // 清除邮箱到用户ID的映射缓存
            String emailCacheKey = "user:email:" + email;
            redisService.delete(emailCacheKey);
            
            System.out.println("成功清除用户 " + userId + " 的所有缓存信息");
        } catch (Exception e) {
            System.err.println("清除用户缓存时出错: " + e.getMessage());
        }
    }

    /**
     * 更新用户头像
     * 采用延迟写入策略：先更新缓存，用户退出时再写入数据库
     */
    @Transactional
    public UpdateResult updateUserAvatar(Integer userId, UpdateAvatarRequest request) {
        try {
            // 检查用户是否存在
            if (!userInfoRepository.existsById(userId)) {
                return UpdateResult.failure("用户不存在");
            }
            
            // 延迟写入策略：先更新Redis缓存
            redisService.updateUserInfoAvatar(userId, request.getAvatarUrl());
            
            System.out.println("用户 " + userId + " 头像已更新到缓存，等待退出时写入数据库");
            return UpdateResult.success("头像更新成功（缓存中）");
            
        } catch (Exception e) {
            return UpdateResult.failure("更新失败：" + e.getMessage());
        }
    }

    /**
     * 将用户缓存中的修改同步到数据库
     * 在用户退出登录时调用
     */
    @Transactional
    public void syncCacheToDatabase(Integer userId) {
        try {
            UserInfoDto cachedUser = redisService.getUserLoginInfo(userId);
            
            if (cachedUser != null && cachedUser.hasModifications()) {
                System.out.println("开始同步用户 " + userId + " 的缓存修改到数据库，修改字段: " + cachedUser.getModifiedFields());
                
                // 同步基本信息修改
                if (cachedUser.isFieldModified("username") || 
                    cachedUser.isFieldModified("phone") || 
                    cachedUser.isFieldModified("note")) {
                    
                    int updated = userInfoRepository.updateBasicInfo(
                        userId, 
                        cachedUser.getUsername(), 
                        cachedUser.getPhone(), 
                        cachedUser.getNote()
                    );
                    
                    if (updated > 0) {
                        System.out.println("用户 " + userId + " 基本信息已同步到数据库");
                    }
                }
                
                // 同步头像修改
                if (cachedUser.isFieldModified("avatar")) {
                    int updated = userInfoRepository.updateAvatar(userId, cachedUser.getAvatar());
                    if (updated > 0) {
                        System.out.println("用户 " + userId + " 头像已同步到数据库");
                    }
                }
                
                // 清除修改标记
                cachedUser.clearModifications();
                redisService.storeUserLoginInfo(userId, cachedUser);
                
                System.out.println("用户 " + userId + " 所有缓存修改已成功同步到数据库");
            } else {
                System.out.println("用户 " + userId + " 缓存中无修改，跳过数据库同步");
            }
            
        } catch (Exception e) {
            System.err.println("同步用户 " + userId + " 缓存到数据库时出错: " + e.getMessage());
        }
    }

    // ================================
    // 用户查询方法
    // ================================

    /**
     * 根据用户ID获取用户信息
     */
    public UserInfo findUserById(Integer userId) {
        return userInfoRepository.findById(userId).orElse(null);
    }

    /**
     * 根据用户名或邮箱查找用户
     */
    public UserInfo findUserByEmail(String userEmail) {
        return userInfoRepository.findByEmail(userEmail)
                .orElse(null);
    }

    /**
     * 获取用户详细信息（新版本 - 优先从Redis获取）
     * 优先从Redis缓存获取，如果没有则从数据库加载并存入Redis
     */
    public UserInfoDto getUserInfo(Integer userId) {
        // 1. 先尝试从Redis获取
        UserInfoDto cachedUserInfo = redisService.getUserInfo(userId);
        if (cachedUserInfo != null) {
            // 刷新缓存过期时间
            redisService.refreshUserInfo(userId);
            return cachedUserInfo;
        }
        
        // 2. Redis中没有，从数据库加载
        UserInfo userInfo = findUserById(userId);
        if (userInfo == null) {
            return null;
        }
        
        // 3. 转换为DTO
        UserInfoDto userInfoDto = convertToUserInfoDto(userInfo);
        
        // 4. 存入Redis缓存
        redisService.storeUserInfo(userId, userInfoDto);
        
        return userInfoDto;
    }
    
    /**
     * 将UserInfo实体转换为UserInfoDto
     */
    private UserInfoDto convertToUserInfoDto(UserInfo userInfo) {
        return UserInfoDto.builder()
            .userId(userInfo.getUserId())
            .username(userInfo.getUsername())
            .email(userInfo.getEmail())
            .phone(userInfo.getPhone())
            .avatar(userInfo.getAvatar())
            .note(userInfo.getNote())
            .role(userInfo.getRole().name())
            .creditScore(userInfo.getCreditScore())
            .dealTime(userInfo.getDealTime())
            .creditTime(userInfo.getCreditTime())
            .status(userInfo.getStatus())
            .modifiedFields(new HashSet<>()) // 新创建的DTO没有修改字段
            .build();
    }

    /**
     * 获取用户详细信息（DTO格式）- 兼容旧版本，重定向到新方法
     */
    public UserInfoDto getUserProfile(Integer userId) {
        return getUserInfo(userId);
    }

    /**
     * 获取用户登录信息（DTO格式）
     */
    public UserInfoDto getUserLoginInfo(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        
        return UserInfoDto.builder()
            .userId(userInfo.getUserId())
            .username(userInfo.getUsername())
            .email(userInfo.getEmail())
            .role(userInfo.getRole().name())
            .avatar(userInfo.getAvatar())
            .creditScore(userInfo.getCreditScore())
            .phone(userInfo.getPhone())
            .note(userInfo.getNote())
            .dealTime(userInfo.getDealTime())
            .creditTime(userInfo.getCreditTime())
            .status(userInfo.getStatus())
            .modifiedFields(new HashSet<>())  // 初始化为空集合
            .build();
    }

    // ================================
    // 密码相关方法
    // ================================

    /**
     * 验证用户密码（通过用户ID）
     */
    public boolean validateUserPassword(Integer userId, String rawPassword) {
        // 第一步：检查用户状态
        UserInfo userInfo = findUserById(userId);
        if (userInfo == null || !userInfo.getStatus()) {
            return false;
        }
        
        // 第二步：查找密码
        Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUserId(userId);
        if (userPasswordOpt.isEmpty()) {
            return false;
        }
        
        // 验证密码
        return passwordEncoder.matches(rawPassword, userPasswordOpt.get().getPassword());
    }

    /**
     * 验证用户密码（通过用户名或邮箱）
     */
    public boolean validateUserPasswordByEmail (String userEmail, String rawPassword) {
        // 第一步：通过用户名或邮箱找到用户信息
        UserInfo userInfo = findUserByEmail(userEmail);
        if (userInfo == null || !userInfo.getStatus()) {
            return false;
        }
        
        // 第二步：通过用户ID查找密码
        Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUserId(userInfo.getUserId());
        if (userPasswordOpt.isEmpty()) {
            return false;
        }
        
        // 验证密码
        return passwordEncoder.matches(rawPassword, userPasswordOpt.get().getPassword());
    }

    /**
     * 更新用户密码
     */
    @Transactional
    public UpdateResult updateUserPassword(Integer userId, String oldPassword, String newPassword) {
        try {
            // 验证旧密码
            if (!validateUserPassword(userId, oldPassword)) {
                return UpdateResult.failure("原密码错误");
            }
            
            // 加密新密码并更新
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            int updated = userPasswordRepository.updatePasswordByUserId(userId, encodedNewPassword);
            
            if (updated > 0) {
                return UpdateResult.success("密码更新成功");
            } else {
                return UpdateResult.failure("密码更新失败");
            }
            
        } catch (Exception e) {
            return UpdateResult.failure("密码更新失败：" + e.getMessage());
        }
    }

    /**
     * 创建用户密码（用于注册）
     */
    @Transactional
    public UserPassword createUserPassword(Integer userId, String rawPassword) {
        // 检查用户是否存在
        UserInfo userInfo = findUserById(userId);
        if (userInfo == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查是否已有密码
        if (userPasswordRepository.existsByUserId(userId)) {
            throw new RuntimeException("用户已设置密码");
        }
        
        // 创建密码记录
        String encodedPassword = passwordEncoder.encode(rawPassword);
        UserPassword userPassword = new UserPassword(userId, encodedPassword);
        return userPasswordRepository.save(userPassword);
    }

    /**
     * 重置用户密码（忘记密码用）
     */
    @Transactional
    public boolean resetUserPassword(Integer userId, String newPassword) {
        try {
            // 检查用户是否存在
            if (!userInfoRepository.existsById(userId)) {
                System.err.println("重置密码失败：用户 " + userId + " 不存在");
                return false;
            }
            
            // 加密新密码
            String hashedPassword = passwordEncoder.encode(newPassword);
            
            // 查找用户密码记录
            Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUserId(userId);
            
            if (userPasswordOpt.isPresent()) {
                // 更新现有密码
                UserPassword userPassword = userPasswordOpt.get();
                userPassword.setPassword(hashedPassword);
                userPasswordRepository.save(userPassword);
                System.out.println("用户 " + userId + " 密码重置成功");
            } else {
                // 创建新的密码记录
                UserPassword userPassword = new UserPassword(userId, hashedPassword);
                userPasswordRepository.save(userPassword);
                System.out.println("为用户 " + userId + " 创建新密码记录成功");
            }
            
            // 清除该用户的所有Redis缓存，强制重新登录
            UserInfo userInfo = userInfoRepository.findById(userId).orElse(null);
            if (userInfo != null) {
                clearUserCache(userId, userInfo.getEmail());
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("重置用户 " + userId + " 密码失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查用户是否已设置密码
     */
    public boolean hasUserSetPassword(Integer userId) {
        return userPasswordRepository.existsByUserId(userId);
    }

    // ================================
    // 内部结果类和DTO类
    // ================================

    /**
     * 更新操作结果类
     */
    public static class UpdateResult {
        private boolean success;
        private String message;
        private Object data;
        
        private UpdateResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        public static UpdateResult success(String message) {
            return new UpdateResult(true, message, null);
        }
        
        public static UpdateResult success(String message, Object data) {
            return new UpdateResult(true, message, data);
        }
        
        public static UpdateResult failure(String message) {
            return new UpdateResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }

}
