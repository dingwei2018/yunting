package com.yunting.common;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应体
 */
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final T data;
    private final long timestamp;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        // 确保 data 不为 null，默认为空字符串
        this.data = data != null ? data : getDefaultData();
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        // 双重保护：确保返回的 data 不为 null
        return data != null ? data : getDefaultData();
    }

    @SuppressWarnings("unchecked")
    private T getDefaultData() {
        // 返回空字符串作为默认值
        return (T) "";
    }

    public long getTimestamp() {
        return timestamp;
    }
}

