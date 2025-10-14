package com.SE2025BackEnd_16.project.dto;

import lombok.Data;

/**
 * 更新操作结果DTO
 */
@Data
public class UpdateResult {
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
}
