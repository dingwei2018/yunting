package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.reading.ReadingRuleApplyResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleDTO;
import com.yunting.dto.reading.ReadingRuleListResponseDTO;
import com.yunting.service.ReadingRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading-rules")
public class ReadingRuleController {

    private final ReadingRuleService readingRuleService;

    public ReadingRuleController(ReadingRuleService readingRuleService) {
        this.readingRuleService = readingRuleService;
    }

    @PostMapping
    public ApiResponse<ReadingRuleDTO> createReadingRule(@RequestBody ReadingRuleCreateRequest request) {
        ReadingRuleDTO dto = readingRuleService.createReadingRule(request);
        return ResponseUtil.success(dto);
    }

    @GetMapping
    public ApiResponse<ReadingRuleListResponseDTO> getReadingRules(
            @RequestParam(value = "task_id", required = false) Long taskId,
            @RequestParam(value = "scope", required = false) Integer scope) {
        ReadingRuleListResponseDTO list = readingRuleService.getReadingRules(taskId, scope);
        return ResponseUtil.success(list);
    }

    @PostMapping("/apply")
    public ApiResponse<ReadingRuleApplyResponseDTO> applyReadingRule(
            @RequestParam("ruleid") Long ruleId,
            @RequestParam("taskid") Long taskId) {
        ReadingRuleApplyResponseDTO dto = readingRuleService.applyReadingRule(ruleId, taskId);
        return ResponseUtil.success(dto);
    }
}


