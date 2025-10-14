package com.SE2025BackEnd_16.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {
    
    private List<T> content; // 当前页数据
    private Integer page; // 当前页码
    private Integer size; // 每页大小
    private Long totalElements; // 总记录数
    private Integer totalPages; // 总页数
    private Boolean first; // 是否为第一页
    private Boolean last; // 是否为最后一页
    private Boolean empty; // 是否为空
    
    /**
     * 创建分页响应
     */
    public static <T> PageResponseDTO<T> of(List<T> content, Integer page, Integer size, Long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PageResponseDTO.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content == null || content.isEmpty())
                .build();
    }
    
    /**
     * 创建空的分页响应
     */
    public static <T> PageResponseDTO<T> empty(Integer page, Integer size) {
        return PageResponseDTO.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return !last && page < totalPages - 1;
    }
    
    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return page > 0;
    }
    
    /**
     * 获取下一页页码
     */
    public Integer getNextPage() {
        return hasNext() ? page + 1 : null;
    }
    
    /**
     * 获取上一页页码
     */
    public Integer getPreviousPage() {
        return hasPrevious() ? page - 1 : null;
    }
    
    /**
     * 获取当前页码（兼容性方法）
     */
    public Integer getCurrentPage() {
        return page;
    }
} 