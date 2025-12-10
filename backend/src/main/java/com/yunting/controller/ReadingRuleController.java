package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.reading.MatchingFieldListResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleCreateResponseDTO;
import com.yunting.dto.reading.ReadingRuleListPageResponseDTO;
import com.yunting.dto.reading.ReadingRuleSetGlobalSettingRequest;
import com.yunting.dto.reading.ReadingRuleSetGlobalSettingResponseDTO;
import com.yunting.service.ReadingRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 开关全局阅读规范
     *
     * @param request 开关全局阅读规范请求
     * @return 设置结果，包含过滤的断句列表
     */
    @PostMapping("/setGlobalSetting")
    public ApiResponse<ReadingRuleSetGlobalSettingResponseDTO> setGlobalSetting(@Valid @RequestBody ReadingRuleSetGlobalSettingRequest request) {
        ReadingRuleSetGlobalSettingResponseDTO data = readingRuleService.setGlobalSetting(request);
        return ResponseUtil.success(data);
    }

    /**
     * 获取阅读规范列表（支持分页和筛选）
     *
     * @param taskId 任务ID（可选）
     * @param ruleType 规则类型（可选）
     * @param page 页码（必填）
     * @param pageSize 每页大小（必填）
     * @return 阅读规范列表
     */
    @GetMapping("/getList")
    public ApiResponse<ReadingRuleListPageResponseDTO> getReadingRuleList(
            @RequestParam(value = "task_id", required = false) Long taskId,
            @RequestParam(value = "ruleType", required = false) Integer ruleType,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        ReadingRuleListPageResponseDTO data = readingRuleService.getReadingRuleList(taskId, ruleType, page, pageSize);
        return ResponseUtil.success(data);
    }

    /**
     * 获取文本中符合规则的字段列表
     *
     * @param text 文本内容（可选）
     * @return 匹配的字段列表
     */
    @GetMapping("/getMatchingFieldListFromText")
    public ApiResponse<MatchingFieldListResponseDTO> getMatchingFieldListFromText(
            @RequestParam(value = "text", required = false) String text) {
        MatchingFieldListResponseDTO data = readingRuleService.getMatchingFieldListFromText(text);
        return ResponseUtil.success(data);
    }
}
