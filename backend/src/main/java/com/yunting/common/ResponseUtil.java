package com.yunting.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一响应工具
 */
public final class ResponseUtil {

    private static final int SUCCESS_CODE = 10200;
    private static final int DEFAULT_ERROR_CODE = 10500;

    private static final Map<String, Object> EMPTY_DATA = new LinkedHashMap<>();
    private static final String EMPTY_STRING = "";

    private ResponseUtil() {
    }

    public static <T> ApiResponse<T> success() {
        // 返回空字符串而不是 null
        return success((T) EMPTY_STRING);
    }

    public static <T> ApiResponse<T> success(T data) {
        // 如果 data 为 null，使用空字符串作为默认值
        T safeData = data != null ? data : (T) EMPTY_STRING;
        return ApiResponse.of(SUCCESS_CODE, "success", safeData);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // 如果 data 为 null，使用空字符串作为默认值
        T safeData = data != null ? data : (T) EMPTY_STRING;
        return ApiResponse.of(SUCCESS_CODE, message, safeData);
    }

    public static <T> ApiResponse<T> error(String message) {
        // 错误响应也返回空字符串而不是 null
        return ApiResponse.of(DEFAULT_ERROR_CODE, message, (T) EMPTY_STRING);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        // 错误响应也返回空字符串而不是 null
        return ApiResponse.of(code, message, (T) EMPTY_STRING);
    }

    /**
     * 返回错误响应，data为空对象
     */
    public static ApiResponse<Map<String, Object>> errorWithEmptyData(String message) {
        return ApiResponse.of(DEFAULT_ERROR_CODE, message, EMPTY_DATA);
    }

    /**
     * 返回错误响应，data为空对象
     */
    public static ApiResponse<Map<String, Object>> errorWithEmptyData(int code, String message) {
        return ApiResponse.of(code, message, EMPTY_DATA);
    }
}

