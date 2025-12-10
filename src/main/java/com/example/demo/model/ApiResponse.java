package com.example.demo.model;

import java.time.Instant;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    private ErrorDetail error;

    public ApiResponse() {
        this.timestamp = Instant.now().getEpochSecond();
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().getEpochSecond();
    }

    public ApiResponse(int code, String message, T data, ErrorDetail error) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = Instant.now().getEpochSecond();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, ErrorDetail error) {
        return new ApiResponse<>(code, message, null, error);
    }

    public static class ErrorDetail {
        private String type;
        private String detail;

        public ErrorDetail(String type, String detail) {
            this.type = type;
            this.detail = detail;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }
}