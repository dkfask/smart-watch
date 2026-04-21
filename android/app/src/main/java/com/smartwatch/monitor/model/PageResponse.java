package com.smartwatch.monitor.model;

import java.util.List;

/**
 * 通用分页响应模型
 * @param <T> 数据类型
 */
public class PageResponse<T> {

    /** 数据列表 */
    private List<T> content;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页数量 */
    private int size;

    /** 总页数 */
    private int totalPages;

    /**
     * 默认构造函数
     */
    public PageResponse() {}

    /**
     * 带参数的构造函数
     * @param content 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页数量
     */
    public PageResponse(List<T> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
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
