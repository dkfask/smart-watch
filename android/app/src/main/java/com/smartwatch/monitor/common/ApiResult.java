package com.smartwatch.monitor.common;

public class ApiResult<T> {
    private final T data;
    private final String error;
    private final boolean retryable;

    private ApiResult(T data, String error, boolean retryable) {
        this.data = data;
        this.error = error;
        this.retryable = retryable;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(data, null, false);
    }

    public static <T> ApiResult<T> error(String error, boolean retryable) {
        return new ApiResult<>(null, error, retryable);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
