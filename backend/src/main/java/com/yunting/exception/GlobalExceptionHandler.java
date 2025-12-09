package com.yunting.exception;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseUtil.errorWithEmptyData(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Map<String, Object>> handleValidationException(Exception ex) {
        String message = "请求参数不合法";
        if (ex instanceof MethodArgumentNotValidException manve && manve.getBindingResult().getFieldError() != null) {
            message = manve.getBindingResult().getFieldError().getDefaultMessage();
        } else if (ex instanceof BindException be && be.getBindingResult().getFieldError() != null) {
            message = be.getBindingResult().getFieldError().getDefaultMessage();
        }
        log.warn("Validation failed: {}", message);
        return ResponseUtil.errorWithEmptyData(10400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseUtil.errorWithEmptyData(10400, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable", ex);
        return ResponseUtil.errorWithEmptyData(10400, "请求参数格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseUtil.errorWithEmptyData(10500, "服务器开小差了，请稍后再试");
    }
}

