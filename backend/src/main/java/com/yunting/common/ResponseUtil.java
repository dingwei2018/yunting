package com.yunting.common;

/**
 * 统一响应工具
 */
public final class ResponseUtil {

    private static final int SUCCESS_CODE = 10200;
    private static final int DEFAULT_ERROR_CODE = 10500;

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
}

