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

    private ResponseUtil() {
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.of(SUCCESS_CODE, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.of(SUCCESS_CODE, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.of(DEFAULT_ERROR_CODE, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.of(code, message, null);
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

