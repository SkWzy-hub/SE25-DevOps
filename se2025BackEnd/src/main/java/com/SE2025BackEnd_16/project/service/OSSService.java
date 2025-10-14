package com.SE2025BackEnd_16.project.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云OSS服务类
 */
@Slf4j
@Service
public class OSSService {
    
    @Autowired
    private OSS ossClient;
    
    @Value("${aliyun.oss.bucketName:se2025-project}")
    private String bucketName;
    
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.oss.urlPrefix:}")
    private String urlPrefix;
    
    /**
     * 上传单个文件
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 验证文件类型
            if (!isImageFile(file)) {
                throw new RuntimeException("只支持上传图片文件");
            }
            // 验证文件大小 (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new RuntimeException("文件大小不能超过5MB");
            }
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String fileName = folder + "/" + UUID.randomUUID().toString() + extension;
            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, fileName, file.getInputStream());
            ossClient.putObject(putObjectRequest);
            // 返回访问URL
            String fileUrl = buildFileUrl(fileName);
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        } catch (OSSException e) {
            log.error("OSS上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage());
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }
    /**
     * 批量上传文件
     */
    public List<String> uploadFiles(List<MultipartFile> files, String folder) {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                // 验证文件类型
                if (!isImageFile(file)) {
                    throw new RuntimeException("只支持上传图片文件");
                }
                // 验证文件大小 (5MB)
                if (file.getSize() > 5 * 1024 * 1024) {
                    throw new RuntimeException("文件大小不能超过5MB");
                }
                String fileUrl = uploadFile(file, folder);
                fileUrls.add(fileUrl);
            }
        }
        return fileUrls;
    }
    /**
     * 验证是否为图片文件
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {
        try {
            String baseUrl = buildFileUrl("");
            if (fileUrl != null && fileUrl.startsWith(baseUrl)) {
                String fileName = fileUrl.substring(baseUrl.length());
                ossClient.deleteObject(bucketName, fileName);
                log.info("文件删除成功: {}", fileUrl);
            }
        } catch (OSSException e) {
            log.error("OSS删除失败: {}", e.getMessage());
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }
    /**
     * 构建文件访问URL
     */
    private String buildFileUrl(String fileName) {
        if (urlPrefix != null && !urlPrefix.isEmpty()) {
            return urlPrefix + "/" + fileName;
        } else {
            // 如果没有配置urlPrefix，则使用bucket和endpoint动态构建
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        }
    }
} 