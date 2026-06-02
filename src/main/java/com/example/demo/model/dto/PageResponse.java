package com.example.demo.model.dto;

import java.util.List;

/**
 * 通用分页响应DTO，统一所有分页接口的响应格式
 * @param <T> 数据类型
 */
public class PageResponse<T> {
    private List<T> content;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse() {}

    public PageResponse(List<T> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }

    /**
     * 从Spring Data Page对象构建PageResponse
     * @param page Spring Data分页结果
     * @return 统一分页响应
     */
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize());
    }

    /**
     * 从List和Pageable构建PageResponse（适用于需要自定义count的场景）
     * @param content 数据列表
     * @param total 总数
     * @param pageable 分页参数
     * @return 统一分页响应
     */
    public static <T> PageResponse<T> from(java.util.List<T> content, long total, org.springframework.data.domain.Pageable pageable) {
        return new PageResponse<>(content, total, pageable.getPageNumber(), pageable.getPageSize());
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
