package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查/健康检查控制器
 * @module 健康检查
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 健康检查
     * 检查服务运行状态
     *
     * @return 服务状态信息，包含 status 和 message 字段
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = Map.of(
                "status", "UP",
                "message", "服务运行正常"
        );
        return ResponseUtil.success(status);
    }
}

