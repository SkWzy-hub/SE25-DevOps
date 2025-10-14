package com.SE2025BackEnd_16.project.service.impl;

import com.SE2025BackEnd_16.project.dto.UserInfoDto;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.entity.UserPassword;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.repository.UserPasswordRepository;
import com.SE2025BackEnd_16.project.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private UserPasswordRepository userPasswordRepository;
    
    @Autowired
    private RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        // 注意：虽然方法名是loadUserByUsername（Spring Security接口要求），
        // 但在我们的系统中，username参数实际上是用户的邮箱地址
        
        UserInfo userInfo = null;
        UserPassword userPassword = null;
        
        // 先尝试从Redis获取缓存的用户基本信息
        String emailCacheKey = "user:email:" + username;
        Object cachedUserId = redisService.get(emailCacheKey);
        
        if (cachedUserId != null) {
            System.out.println("Redis缓存命中: 邮箱 " + username + " -> 用户ID " + cachedUserId);
            // 从Redis缓存获取用户基本信息
            try {
                UserInfoDto  cachedUser = redisService.getUserLoginInfo((Integer) cachedUserId);
                if (cachedUser != null) {
                    System.out.println("Redis缓存命中: 获取到用户基本信息 " + cachedUser.getUsername());
                    
                    // 安全考虑：密码始终从数据库查询，不缓存
                    Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUserId(cachedUser.getUserId());
                    if (userPasswordOpt.isEmpty()) {
                        throw new UsernameNotFoundException("用户密码信息缺失: " + username);
                    }
                    userPassword = userPasswordOpt.get();
                    
                    // 从缓存构建UserDetails对象
                    return User.builder()
                            .username(cachedUser.getUsername())
                            .password(userPassword.getPassword()) // 从数据库获取密码
                            .authorities(Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + cachedUser.getRole().toUpperCase())
                            ))
                            .accountExpired(false)
                            .accountLocked(!cachedUser.getStatus())
                            .credentialsExpired(false)
                            .disabled(!cachedUser.getStatus())
                            .build();
                } else {
                    System.out.println("Redis缓存: 邮箱映射存在但用户详细信息缓存已失效，用户ID: " + cachedUserId);
                }
            } catch (Exception e) {
                System.out.println("Redis缓存数据损坏，清除缓存: " + e.getMessage());
                // 清除损坏的缓存
                redisService.removeUserLoginInfo((Integer) cachedUserId);
                redisService.delete(emailCacheKey);
            }
        } else {
            System.out.println("Redis缓存未命中: 邮箱 " + username + " 没有缓存映射");
        }
        
        // 缓存中没有，从数据库查询
        System.out.println("从数据库查询用户信息: " + username);
        // 第一步：在user_info表中通过邮箱查找用户
        userInfo = userInfoRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 检查用户是否被禁用
        if (!userInfo.getStatus()) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 第二步：通过user_id到user_password表查找密码
        Optional<UserPassword> userPasswordOpt = userPasswordRepository.findByUserId(userInfo.getUserId());
        if (userPasswordOpt.isEmpty()) {
            throw new UsernameNotFoundException("用户密码信息缺失: " + username);
        }
        
        userPassword = userPasswordOpt.get();

        // 将用户基本信息（不包含密码）缓存到Redis
        UserInfoDto userLoginDto = new UserInfoDto();
        userLoginDto.setUserId(userInfo.getUserId());
        userLoginDto.setUsername(userInfo.getUsername());
        userLoginDto.setEmail(userInfo.getEmail());
        userLoginDto.setPhone(userInfo.getPhone());
        userLoginDto.setAvatar(userInfo.getAvatar());
        userLoginDto.setNote(userInfo.getNote());
        userLoginDto.setCreditScore(userInfo.getCreditScore());
        userLoginDto.setDealTime(userInfo.getDealTime());
        userLoginDto.setCreditTime(userInfo.getCreditTime());
        userLoginDto.setRole(userInfo.getRole().name());
        userLoginDto.setStatus(userInfo.getStatus());
        // 安全改进：不再缓存密码到Redis
        
        // 缓存用户基本信息（24小时过期）
        redisService.storeUserLoginInfo(userInfo.getUserId(), userLoginDto);
        // 缓存邮箱到用户ID的映射（24小时过期）
        redisService.set(emailCacheKey, userInfo.getUserId(), 24 * 60 * 60);
        System.out.println("已缓存用户基本信息到Redis（不含密码）: 用户ID " + userInfo.getUserId() + ", 邮箱 " + userInfo.getEmail());

        // 构建Spring Security的UserDetails对象
        return User.builder()
                .username(userInfo.getUsername())
                .password(userPassword.getPassword())
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + userInfo.getRole().name().toUpperCase())
                ))
                .accountExpired(false)
                .accountLocked(!userInfo.getStatus())
                .credentialsExpired(false)
                .disabled(!userInfo.getStatus())
                .build();
    }
}
