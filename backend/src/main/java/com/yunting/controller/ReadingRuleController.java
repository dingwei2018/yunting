package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleCreateResponseDTO;
import com.yunting.service.ReadingRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅读规范控制器
 */
@RestController
@RequestMapping("/api/readingRule")
public class ReadingRuleController {

    private final ReadingRuleService readingRuleService;

    public ReadingRuleController(ReadingRuleService readingRuleService) {
        this.readingRuleService = readingRuleService;
    }

    /**
     * 创建阅读规范
     *
     * @param request 创建阅读规范请求
     * @return 创建结果，包含 ruleId
     */
    @PostMapping("/create")
    public ApiResponse<ReadingRuleCreateResponseDTO> createReadingRule(@Valid @RequestBody ReadingRuleCreateRequest request) {
        ReadingRuleCreateResponseDTO data = readingRuleService.createReadingRule(request);
        return ResponseUtil.success(data);
    }
}
