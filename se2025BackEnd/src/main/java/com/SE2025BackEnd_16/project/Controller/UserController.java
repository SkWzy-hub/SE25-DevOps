package com.SE2025BackEnd_16.project.Controller;

import com.SE2025BackEnd_16.project.dto.request.UserUpdateProfileRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.UserChangePasswordRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ApiResponse;
import com.SE2025BackEnd_16.project.dto.UpdateAvatarRequest;
import com.SE2025BackEnd_16.project.service.UserService;
import com.SE2025BackEnd_16.project.service.OSSService;
import com.SE2025BackEnd_16.project.service.RedisService;
import com.SE2025BackEnd_16.project.dto.UserInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

/**
 * 用户信息管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@Validated
@CrossOrigin(origins = "*") // 允许跨域
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OSSService ossService;
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 获取用户详细信息（新版本 - 优先从Redis获取）
     */
    @GetMapping("/{userId}/info")
    public ResponseEntity<ApiResponse<UserInfoDto>> getUserInfo(@PathVariable Integer userId) {
        try {
            log.info("获取用户信息: userId={}", userId);
            
            // 优先从Redis获取用户信息
            UserInfoDto userInfo = userService.getUserInfo(userId);
            if (userInfo == null) {
                log.warn("用户不存在: userId={}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            
            log.info("成功获取用户信息: userId={}, username={}", userId, userInfo.getUsername());
            return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", userInfo));
        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取用户信息失败"));
        }
    }

    /**
     * 获取用户详细信息（兼容旧版本 - 重定向到新方法）
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserInfoDto>> getUserProfile(@PathVariable Integer userId) {
        try {
            UserInfoDto userProfile = userService.getUserProfile(userId);
            if (userProfile == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(404, "用户不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success(userProfile));
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取用户信息失败"));
        }
    }
    
    /**
     * 更新用户基本信息（用户名、电话、个人签名）
     */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserInfoDto>> updateUserProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody UserUpdateProfileRequestDTO requestDTO) {
        try {
            UserService.UpdateResult result = userService.updateUserBasicInfo(
                    userId, 
                    requestDTO.getUsername(), 
                    requestDTO.getPhone(), 
                    requestDTO.getNote()
            );
            
            if (result.isSuccess()) {
                // 更新成功后，重新获取用户信息
                UserInfoDto userProfile = userService.getUserProfile(userId);
                return ResponseEntity.ok(ApiResponse.success(result.getMessage(), userProfile));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, result.getMessage()));
            }
        } catch (Exception e) {
            log.error("更新用户信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "更新用户信息失败"));
        }
    }
    
    /**
     * 更新用户头像（通过URL）
     */
    @PutMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponse<Void>> updateUserAvatar(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateAvatarRequest requestDTO) {
        try {
            UserService.UpdateResult result = userService.updateUserAvatar(userId, requestDTO);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, result.getMessage()));
            }
        } catch (Exception e) {
            log.error("更新用户头像失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "更新用户头像失败"));
        }
    }
    
    /**
     * 更新用户头像（通过文件上传）
     */
    @PostMapping("/{userId}/avatar/upload")
    public ResponseEntity<ApiResponse<String>> uploadUserAvatar(
            @PathVariable Integer userId,
            @RequestParam("avatar") MultipartFile file) {
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "请选择要上传的头像文件"));
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "只支持上传图片文件"));
            }
            
            // 验证文件大小 (2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "头像文件大小不能超过2MB"));
            }
            
            // 上传文件到OSS
            String avatarUrl = ossService.uploadFile(file, "avatars");
            log.info("用户 {} 头像上传成功: {}", userId, avatarUrl);
            
            // 更新用户头像URL
            UpdateAvatarRequest avatarRequest = new UpdateAvatarRequest();
            avatarRequest.setAvatarUrl(avatarUrl);
            UserService.UpdateResult result = userService.updateUserAvatar(userId, avatarRequest);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success("头像上传成功", avatarUrl));
            } else {
                // 如果更新失败，删除已上传的文件
                try {
                    ossService.deleteFile(avatarUrl);
                } catch (Exception deleteEx) {
                    log.warn("删除上传失败的头像文件时出错: {}", deleteEx.getMessage());
                }
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("上传用户头像失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "头像上传失败: " + e.getMessage()));
        }
    }
    
    /**
     * 修改用户密码
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changeUserPassword(
            @PathVariable Integer userId,
            @Valid @RequestBody UserChangePasswordRequestDTO requestDTO) {
        try {
            // 验证确认密码是否一致
            if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "新密码和确认密码不一致"));
            }
            
            // 验证新密码和旧密码不能相同
            if (requestDTO.getOldPassword().equals(requestDTO.getNewPassword())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "新密码不能与旧密码相同"));
            }
            
            // 调用服务层方法修改密码
            UserService.UpdateResult result = userService.updateUserPassword(
                    userId, 
                    requestDTO.getOldPassword(), 
                    requestDTO.getNewPassword()
            );
            
            if (result.isSuccess()) {
                log.info("用户 {} 密码修改成功", userId);
                return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, result.getMessage()));
            }
            
        } catch (Exception e) {
            log.error("修改用户密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "修改密码失败"));
        }
    }
} 