package com.SE2025BackEnd_16.project.Controller;

import com.SE2025BackEnd_16.project.dto.LoginRequest;
import com.SE2025BackEnd_16.project.dto.LoginResponse;
import com.SE2025BackEnd_16.project.dto.UserInfoDto;
import com.SE2025BackEnd_16.project.dto.request.UserRegisterRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.UserRegisterResponseDTO;
import com.SE2025BackEnd_16.project.dto.response.ApiResponse;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.service.EmailService;
import com.SE2025BackEnd_16.project.service.UserService;
import com.SE2025BackEnd_16.project.service.RedisService;
import com.SE2025BackEnd_16.project.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录相关控制器
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EmailService emailService;

    /**
     * 用户登录（从数据库验证，成功后存储到Redis）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        
        try {
            // 1. 使用邮箱进行认证
            if (!userService.validateUserPasswordByEmail(loginRequest.getEmail(), loginRequest.getPassword())) {
                throw new BadCredentialsException("用户名或密码错误");
            }
            
            // 2. 认证成功，获取用户信息
            UserInfo userInfo = userService.findUserByEmail(loginRequest.getEmail());
            
            if (userInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }

            // 3. 检查用户状态
            if (!userInfo.getStatus()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户账号已被禁用"));
            }

            // 4. 使用用户ID生成JWT Token（username字段存储邮箱）
            String token = jwtUtil.generateTokenWithUserId(
                    userInfo.getUserId(),        // 用用户ID作为subject
                    userInfo.getEmail(),         // JWT中的username字段存储邮箱
                    userInfo.getRole().name()
            );

            // 5. 构造用户登录信息
            UserInfoDto userInfoDto = userService.getUserLoginInfo(userInfo);

            // 6. 将用户信息存储到Redis（用用户ID作为key）
            redisService.storeUserLoginInfo(userInfo.getUserId(), userInfoDto);
            
            // 7. 缓存邮箱到用户ID的映射（用于UserDetailsService快速查找）
            String emailCacheKey = "user:email:" + userInfo.getEmail();
            redisService.set(emailCacheKey, userInfo.getUserId(), 24 * 60 * 60);

            // 8. 构造登录响应
            LoginResponse loginResponse = LoginResponse.success(token, userInfoDto);

            return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));

        } catch (AuthenticationException e) {
            // 认证失败（用户名或密码错误）
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("用户名或密码错误"));
            
        } catch (Exception e) {
            // 其他异常
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "登录失败：" + e.getMessage()));
        }
    }

    /**
     * 获取当前登录用户信息（从Redis获取）
     */
    @GetMapping("/user/current")
    public ResponseEntity<ApiResponse<UserInfoDto>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // 1. 从Authorization header中提取token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("缺少认证token"));
            }
            
            String token = authHeader.substring(7);
            
            // 2. 验证token并获取用户ID
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(401, "token无效或已过期"));
            }
            
            // 3. 从token中获取用户ID
            Integer userId = jwtUtil.getUserIdFromSubject(token);
            
            // 4. 从Redis获取用户信息
            UserInfoDto userInfoDto = redisService.getUserLoginInfo(userId);
            if (userInfoDto == null) {
                // Redis中没有，从数据库查询并重新存储
                UserInfo userInfo = userService.findUserById(userId);
                if (userInfo == null) {
                    return ResponseEntity.status(401)
                            .body(ApiResponse.error(401, "用户不存在"));
                }
                userInfoDto = userService.getUserLoginInfo(userInfo);
                redisService.storeUserLoginInfo(userId, userInfoDto);
            }
            
            return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", userInfoDto));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "获取用户信息失败：" + e.getMessage()));
        }
    }

    /**
     * 用户登出（同步缓存修改到数据库，然后删除Redis中的用户信息）
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // 从token获取用户ID
                Integer userId = jwtUtil.getUserIdFromSubject(token);
                
                // 获取用户信息，用于清除邮箱映射
                UserInfoDto userInfoDto = redisService.getUserLoginInfo(userId);
                
                // 关键：在清除缓存前，先将修改同步到数据库
                userService.syncCacheToDatabase(userId);
                
                // 从Redis删除用户登录信息
                redisService.removeUserLoginInfo(userId);
                
                // 从Redis删除用户详细信息缓存
                redisService.removeUserInfo(userId);
                
                // 清除邮箱到用户ID的映射缓存
                if (userInfoDto != null && userInfoDto.getEmail() != null) {
                    String emailCacheKey = "user:email:" + userInfoDto.getEmail();
                    redisService.delete(emailCacheKey);
                }
                
                System.out.println("用户 ID: " + userId + " 已登出，缓存修改已同步到数据库，Redis信息已清除");
            }
            
            return ResponseEntity.ok(ApiResponse.success("登出成功", null));
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("登出成功", null));
        }
    }

    /**
     * 页面关闭时自动登出（专门用于navigator.sendBeacon调用）
     * 接收token作为请求体参数，因为sendBeacon无法设置自定义header
     */
    @PostMapping("/logout-on-close")
    public ResponseEntity<ApiResponse<String>> logoutOnClose(@RequestBody(required = false) String tokenData) {
        try {
            String token = null;
            
            System.out.println("收到页面关闭登出请求，原始数据: " + tokenData); // 调试日志
            
            // 尝试从请求体中解析token
            if (tokenData != null && !tokenData.trim().isEmpty()) {
                try {
                    // 尝试解析JSON格式: {"token":"actual_token"}
                    if (tokenData.contains("\"token\"")) {
                        // 简单的JSON解析
                        int tokenStart = tokenData.indexOf("\"token\"");
                        int colonIndex = tokenData.indexOf(":", tokenStart);
                        int valueStart = tokenData.indexOf("\"", colonIndex) + 1;
                        int valueEnd = tokenData.indexOf("\"", valueStart);
                        
                        if (valueStart > 0 && valueEnd > valueStart) {
                            token = tokenData.substring(valueStart, valueEnd);
                        }
                    } else {
                        // 如果不是JSON格式，直接使用原始数据
                        token = tokenData.trim();
                    }
                    
                    // 移除可能的Bearer前缀
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    
                    System.out.println("解析出的token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null")); // 调试日志
                    
                } catch (Exception parseError) {
                    System.out.println("token解析失败: " + parseError.getMessage());
                    token = null;
                }
            }
            
            if (token != null && !token.isEmpty()) {
                try {
                    System.out.println("开始验证token..."); // 调试日志
                    // 验证token
                    if (jwtUtil.validateToken(token)) {
                        // 从token获取用户ID
                        Integer userId = jwtUtil.getUserIdFromSubject(token);
                        System.out.println("Token验证成功，用户ID: " + userId); // 调试日志
                        
                        // 获取用户信息，用于清除邮箱映射
                        UserInfoDto userLoginDto = redisService.getUserLoginInfo(userId);
                        System.out.println("获取Redis中的用户信息: " + (userLoginDto != null ? "存在" : "不存在")); // 调试日志
                        
                        // 关键：在清除缓存前，先将修改同步到数据库
                        System.out.println("开始同步缓存到数据库..."); // 调试日志
                        userService.syncCacheToDatabase(userId);
                        
                        // 从Redis删除用户登录信息
                        System.out.println("开始删除Redis用户登录信息..."); // 调试日志
                        redisService.removeUserLoginInfo(userId);
                        
                        // 从Redis删除用户详细信息缓存
                        System.out.println("开始删除Redis用户详细信息缓存..."); // 调试日志
                        redisService.removeUserInfo(userId);
                        
                        // 清除邮箱到用户ID的映射缓存
                        if (userLoginDto != null && userLoginDto.getEmail() != null) {
                            String emailCacheKey = "user:email:" + userLoginDto.getEmail();
                            System.out.println("清除邮箱映射缓存: " + emailCacheKey); // 调试日志
                            redisService.delete(emailCacheKey);
                        }
                        
                        // 验证清理结果
                        boolean isStillOnline = redisService.isUserOnline(userId);
                        System.out.println("页面关闭自动登出完成 - 用户 ID: " + userId + "，Redis清理结果: " + (isStillOnline ? "失败，用户仍在线" : "成功，用户已离线"));
                    } else {
                        System.out.println("Token验证失败"); // 调试日志
                    }
                } catch (Exception e) {
                    System.out.println("页面关闭自动登出失败: " + e.getMessage());
                    e.printStackTrace(); // 打印完整错误堆栈
                }
            } else {
                System.out.println("无效的token，跳过登出处理"); // 调试日志
            }
            
            return ResponseEntity.ok(ApiResponse.success("自动登出成功", null));
            
        } catch (Exception e) {
            // 即使出错也返回成功，避免影响页面关闭
            return ResponseEntity.ok(ApiResponse.success("自动登出完成", null));
        }
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/user/online/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> checkUserOnline(@PathVariable Integer userId) {
        try {
            UserInfoDto userLoginDto = redisService.getUserLoginInfo(userId);
            boolean isOnline = (userLoginDto != null);
            
            return ResponseEntity.ok(ApiResponse.success("查询成功", isOnline));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "查询失败：" + e.getMessage()));
        }
    }

    // ================================
    // 用户注册相关API
    // ================================

    /**
     * 发送注册验证码
     */
    @PostMapping("/register/send-code")
    public ResponseEntity<ApiResponse<String>> sendRegistrationCode(@RequestBody String emailJson) {
        try {
            // 解析邮箱
            String email = parseEmailFromJson(emailJson);
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请提供有效的邮箱地址"));
            }

            // 邮箱格式验证
            if (!email.matches("^[a-zA-Z0-9._%+-]+@sjtu\\.edu\\.cn$")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请使用上海交通大学邮箱（@sjtu.edu.cn）"));
            }

            // 检查邮箱是否已被注册
            if (userService.isEmailRegistered(email)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("该邮箱已被注册"));
            }

            // 生成6位数验证码
            String verificationCode = generateVerificationCode();
            
            // 将验证码存储到Redis，有效期10分钟
            String cacheKey = "registration_code:" + email;
            redisService.set(cacheKey, verificationCode, 10 * 60);
            
            // 发送邮件
            emailService.sendRegistrationCode(email, verificationCode);
            
            return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的邮箱", null));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "发送验证码失败：" + e.getMessage()));
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegisterResponseDTO>> register(@Valid @RequestBody UserRegisterRequestDTO requestDTO) {
        try {
            // 1. 验证确认密码是否一致
            if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("两次输入的密码不一致"));
            }

            // 2. 验证验证码
            String cacheKey = "registration_code:" + requestDTO.getEmail();
            String cachedCode = (String) redisService.get(cacheKey);
            
            if (cachedCode == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("验证码已过期，请重新获取"));
            }
            
            if (!cachedCode.equals(requestDTO.getVerificationCode())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("验证码错误"));
            }

            // 3. 执行注册
            UserInfo userInfo = userService.registerUser(
                    requestDTO.getUsername(),
                    requestDTO.getEmail(),
                    requestDTO.getPhone(),
                    requestDTO.getPassword()
            );

            // 4. 删除验证码（一次性使用）
            redisService.delete(cacheKey);

            // 5. 构造响应
            UserRegisterResponseDTO responseDTO = UserRegisterResponseDTO.success(
                    userInfo.getUserId(),
                    userInfo.getUsername(),
                    userInfo.getEmail(),
                    userInfo.getPhone(),
                    userInfo.getRole().name()
            );

            return ResponseEntity.ok(ApiResponse.success("注册成功", responseDTO));
            
        } catch (RuntimeException e) {
            // 业务异常（如邮箱已存在等）
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            // 系统异常
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "注册失败：" + e.getMessage()));
        }
    }

    // ================================
    // 忘记密码相关API
    // ================================

    /**
     * 发送忘记密码验证码
     */
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordCode(@RequestBody String emailJson) {
        try {
            // 解析邮箱
            String email = parseEmailFromJson(emailJson);
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请提供有效的邮箱地址"));
            }

            // 检查用户是否存在
            UserInfo userInfo = userService.findUserByEmail(email);
            if (userInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("该邮箱未注册"));
            }

            // 生成6位数验证码
            String verificationCode = generateVerificationCode();
            
            // 将验证码存储到Redis，有效期10分钟
            String cacheKey = "forgot_password_code:" + email;
            redisService.set(cacheKey, verificationCode, 10 * 60);
            
            // 发送邮件
            emailService.sendForgotPasswordCode(email, verificationCode);
            
            return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的邮箱", null));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "发送验证码失败：" + e.getMessage()));
        }
    }

    /**
     * 验证忘记密码验证码
     */
    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<ApiResponse<String>> verifyForgotPasswordCode(@RequestBody String requestJson) {
        try {
            // 解析请求参数
            Map<String, String> params = parseJsonToMap(requestJson);
            String email = params.get("email");
            String code = params.get("code");
            
            if (email == null || code == null || email.trim().isEmpty() || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请提供邮箱和验证码"));
            }

            // 从Redis获取验证码
            String cacheKey = "forgot_password_code:" + email;
            String cachedCode = (String) redisService.get(cacheKey);
            
            if (cachedCode == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("验证码已过期，请重新获取"));
            }
            
            if (!cachedCode.equals(code)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("验证码错误"));
            }
            
            // 验证成功，生成临时重置令牌
            String resetToken = generateResetToken();
            String resetTokenKey = "reset_token:" + resetToken;
            
            // 存储重置令牌，有效期30分钟，关联邮箱
            redisService.set(resetTokenKey, email, 30 * 60);
            
            return ResponseEntity.ok(ApiResponse.success("验证码验证成功", resetToken));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "验证失败：" + e.getMessage()));
        }
    }

    /**
     * 通过邮箱重置密码
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<String>> resetPasswordByEmail(@RequestBody String requestJson) {
        try {
            // 解析请求参数
            Map<String, String> params = parseJsonToMap(requestJson);
            String resetToken = params.get("resetToken");
            String newPassword = params.get("newPassword");
            
            if (resetToken == null || newPassword == null || 
                resetToken.trim().isEmpty() || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("请提供完整的重置信息"));
            }

            // 验证重置令牌
            String resetTokenKey = "reset_token:" + resetToken;
            String email = (String) redisService.get(resetTokenKey);
            
            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("重置令牌无效或已过期"));
            }
            
            // 查找用户
            UserInfo userInfo = userService.findUserByEmail(email);
            if (userInfo == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("用户不存在"));
            }
            
            // 重置密码
            boolean success = userService.resetUserPassword(userInfo.getUserId(), newPassword);
            if (success) {
                // 删除重置令牌
                redisService.delete(resetTokenKey);
                
                // 删除验证码记录（如果存在），避免验证码被重复使用
                String forgotPasswordCodeKey = "forgot_password_code:" + email;
                redisService.delete(forgotPasswordCodeKey);
                
                return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(500, "密码重置失败"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "重置密码失败：" + e.getMessage()));
        }
    }

    /**
     * 清除所有用户登录缓存（调试用）
     */
    @PostMapping("/debug/clear-cache")
    public ResponseEntity<ApiResponse<String>> clearCache() {
        try {
            // 这里可以实现清除缓存的逻辑，用于调试
            return ResponseEntity.ok(ApiResponse.success("缓存已清除", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "清除缓存失败：" + e.getMessage()));
        }
    }

    // Helper methods for forgot password
    private String parseEmailFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            if (json.contains("\"email\"")) {
                int emailStart = json.indexOf("\"email\"");
                int colonIndex = json.indexOf(":", emailStart);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    return json.substring(valueStart, valueEnd);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing email from JSON: " + e.getMessage());
        }
        return null;
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000; // 生成6位数验证码
        return String.valueOf(code);
    }

    private String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 20 bytes for a 32-character token
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().encodeToString(bytes);
    }

    private Map<String, String> parseJsonToMap(String json) {
        Map<String, String> params = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return params;
        }
        try {
            // 移除可能的Bearer前缀
            if (json.startsWith("Bearer ")) {
                json = json.substring(7);
            }

            // 尝试解析JSON格式: {"email":"user@example.com","code":"123456"}
            if (json.contains("\"email\"")) {
                int emailStart = json.indexOf("\"email\"");
                int colonIndex = json.indexOf(":", emailStart);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    params.put("email", json.substring(valueStart, valueEnd));
                }
            }
            if (json.contains("\"code\"")) {
                int codeStart = json.indexOf("\"code\"");
                int colonIndex = json.indexOf(":", codeStart);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    params.put("code", json.substring(valueStart, valueEnd));
                }
            }
            if (json.contains("\"resetToken\"")) {
                int resetTokenStart = json.indexOf("\"resetToken\"");
                int colonIndex = json.indexOf(":", resetTokenStart);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    params.put("resetToken", json.substring(valueStart, valueEnd));
                }
            }
            if (json.contains("\"newPassword\"")) {
                int newPasswordStart = json.indexOf("\"newPassword\"");
                int colonIndex = json.indexOf(":", newPasswordStart);
                int valueStart = json.indexOf("\"", colonIndex) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    params.put("newPassword", json.substring(valueStart, valueEnd));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
        return params;
    }
}
