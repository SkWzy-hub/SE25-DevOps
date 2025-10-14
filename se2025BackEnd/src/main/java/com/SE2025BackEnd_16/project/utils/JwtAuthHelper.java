package com.SE2025BackEnd_16.project.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * JWT认证帮助类
 * 用于从HTTP请求中提取当前用户信息
 */
@Component
public class JwtAuthHelper {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 从HTTP请求中获取当前用户ID
     * @param request HTTP请求对象
     * @return 用户ID，如果无法获取则返回null
     */
    public Integer getCurrentUserId(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        
        try {
            return jwtUtil.getUserIdFromSubject(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从HTTP请求中获取当前用户邮箱
     * @param request HTTP请求对象
     * @return 用户邮箱，如果无法获取则返回null
     */
    public String getCurrentUserEmail(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        
        try {
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从HTTP请求中提取JWT token
     * @param request HTTP请求对象
     * @return JWT token，如果没有则返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * 验证当前用户是否有权限访问指定用户的资源
     * @param request HTTP请求对象
     * @param targetUserId 目标用户ID
     * @return true表示有权限，false表示无权限
     */
    public boolean hasPermissionToAccessUser(HttpServletRequest request, Integer targetUserId) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return false;
        }
        
        // 用户只能访问自己的资源
        return currentUserId.equals(targetUserId);
    }
} 