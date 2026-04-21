package com.smartwatch.monitor.model;

/**
 * 通用API响应模型
 * @param <T> 数据类型
 */
public class ApiResponse<T> {

    /** 响应状态码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    /** 错误详情 */
    private ErrorDetail error;

    /**
     * 默认构造函数
     */
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    /**
     * 带参数的构造函数
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public ErrorDetail getError() { return error; }
    public void setError(ErrorDetail error) { this.error = error; }

    /**
     * 错误详情内部类
     */
    public static class ErrorDetail {
        private String type;
        private String detail;

        public ErrorDetail() {}

        public ErrorDetail(String type, String detail) {
            this.type = type;
            this.detail = detail;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
    }
}
