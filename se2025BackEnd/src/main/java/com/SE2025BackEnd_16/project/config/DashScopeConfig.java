package com.SE2025BackEnd_16.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云DashScope AI服务配置类
 * 用于AI商品描述生成功能
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.dashscope")
public class DashScopeConfig {
    
    /**
     * DashScope API Key
     * 在application.properties中配置：aliyun.dashscope.api-key=your-api-key
     */
    private String apiKey;
    
    /**
     * 使用的AI模型名称
     * 默认使用qwen-vl-max模型
     */
    private String model = "qwen-vl-max";
    
    /**
     * 是否启用AI功能（默认启用）
     * 如果设置为false或未配置API Key，则使用预设模板生成描述
     */
    private boolean enabled = true;
    
    /**
     * 检查AI功能是否可用
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
} 