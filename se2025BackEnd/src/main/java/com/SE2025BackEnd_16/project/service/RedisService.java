package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.dto.UserInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis key前缀
    private static final String USER_LOGIN_PREFIX = "user:login:";
    private static final String USER_INFO_PREFIX = "user:info:";
    
    // 默认过期时间（24小时，与JWT一致）
    private static final long DEFAULT_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * 存储用户登录信息到Redis（兼容旧版本 - 重定向到新方法）
     * @param userId 用户ID
     * @param userInfoDto 用户信息
     */
    public void storeUserLoginInfo(Integer userId, UserInfoDto userInfoDto) {
        storeUserInfo(userId, userInfoDto);
    }

    /**
     * 从Redis获取用户登录信息（兼容旧版本 - 重定向到新方法）
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoDto getUserLoginInfo(Integer userId) {
        return getUserInfo(userId);
    }

    /**
     * 更新Redis中用户信息的特定字段（兼容旧版本 - 重定向到新方法）
     * @param userId 用户ID
     * @param username 新用户名
     * @param phone 新电话
     * @param note 新个人签名
     */
    public void updateUserCacheInfo(Integer userId, String username, String phone, String note) {
        updateUserInfo(userId, username, phone, note);
    }

    /**
     * 更新Redis中用户头像（兼容旧版本 - 重定向到新方法）
     * @param userId 用户ID
     * @param avatarUrl 新头像URL
     */
    public void updateUserAvatar(Integer userId, String avatarUrl) {
        updateUserInfoAvatar(userId, avatarUrl);
    }

    /**
     * 删除用户登录信息（登出时使用）
     * @param userId 用户ID
     */
    public void removeUserLoginInfo(Integer userId) {
        String key = USER_LOGIN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Integer userId) {
        String key = USER_LOGIN_PREFIX + userId;
        return redisTemplate.hasKey(key);
    }

    /**
     * 刷新用户登录信息的过期时间
     * @param userId 用户ID
     */
    public void refreshUserLoginInfo(Integer userId) {
        String key = USER_LOGIN_PREFIX + userId;
        redisTemplate.expire(key, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 存储任意数据到Redis
     * @param key 键
     * @param value 值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
    }

    /**
     * 从Redis获取数据
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除Redis中的数据
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // ================================
    // 新的统一用户信息缓存方法
    // ================================
    
    /**
     * 存储用户信息到Redis（统一方法）
     * @param userId 用户ID
     * @param userInfoDto 用户信息
     */
    public void storeUserInfo(Integer userId, UserInfoDto userInfoDto) {
        String key = USER_INFO_PREFIX + userId;
        redisTemplate.opsForValue().set(key, userInfoDto, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        System.out.println("已存储用户 " + userId + " 的信息到Redis缓存");
    }
    
    /**
     * 从Redis获取用户信息（统一方法）
     * 优先从Redis获取，如果没有则返回null
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoDto getUserInfo(Integer userId) {
        String key = USER_INFO_PREFIX + userId;
        Object obj = redisTemplate.opsForValue().get(key);
        UserInfoDto userInfo = obj != null ? (UserInfoDto) obj : null;
        
        if (userInfo != null) {
            System.out.println("从Redis缓存中获取到用户 " + userId + " 的信息");
        } else {
            System.out.println("Redis缓存中未找到用户 " + userId + " 的信息");
        }
        
        return userInfo;
    }
    
    /**
     * 更新Redis中用户信息的特定字段（新版本）
     * @param userId 用户ID
     * @param username 新用户名
     * @param phone 新电话
     * @param note 新个人签名
     */
    public void updateUserInfo(Integer userId, String username, String phone, String note) {
        String key = USER_INFO_PREFIX + userId;
        UserInfoDto cachedUser = getUserInfo(userId);
        
        if (cachedUser != null) {
            // 更新字段并标记为已修改
            if (username != null) {
                cachedUser.setUsernameAndMark(username);
            }
            if (phone != null) {
                cachedUser.setPhoneAndMark(phone);
            }
            if (note != null) {
                cachedUser.setNoteAndMark(note);
            }
            
            // 重新存储到Redis
            redisTemplate.opsForValue().set(key, cachedUser, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
            System.out.println("已更新用户 " + userId + " 的缓存信息，修改字段: " + cachedUser.getModifiedFields());
        }
    }
    
    /**
     * 更新Redis中用户头像（新版本）
     * @param userId 用户ID
     * @param avatarUrl 新头像URL
     */
    public void updateUserInfoAvatar(Integer userId, String avatarUrl) {
        String key = USER_INFO_PREFIX + userId;
        UserInfoDto cachedUser = getUserInfo(userId);
        
        if (cachedUser != null) {
            cachedUser.setAvatarAndMark(avatarUrl);
            // 重新存储到Redis
            redisTemplate.opsForValue().set(key, cachedUser, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
            System.out.println("已更新用户 " + userId + " 的头像缓存");
        }
    }
    
    /**
     * 删除用户信息缓存
     * @param userId 用户ID
     */
    public void removeUserInfo(Integer userId) {
        String key = USER_INFO_PREFIX + userId;
        redisTemplate.delete(key);
        System.out.println("已删除用户 " + userId + " 的信息缓存");
    }
    
    /**
     * 刷新用户信息的过期时间
     * @param userId 用户ID
     */
    public void refreshUserInfo(Integer userId) {
        String key = USER_INFO_PREFIX + userId;
        redisTemplate.expire(key, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 清除所有用户登录缓存（调试用）
     */
    public void clearAllUserCache() {
        String loginPattern = USER_LOGIN_PREFIX + "*";
        String infoPattern = USER_INFO_PREFIX + "*";
        redisTemplate.delete(redisTemplate.keys(loginPattern));
        redisTemplate.delete(redisTemplate.keys(infoPattern));
        System.out.println("已清除所有用户缓存（登录信息和用户信息）");
    }
}
