package com.yunting.exception;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

    private static final int DEFAULT_CODE = 10400;

    private final int code;

    public BusinessException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

