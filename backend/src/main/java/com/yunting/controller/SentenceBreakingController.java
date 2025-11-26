package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.standard.SentenceBreakingSettingRequest;
import com.yunting.dto.standard.SentenceBreakingSettingResponseDTO;
import com.yunting.dto.standard.SentenceBreakingStandardListDTO;
import com.yunting.service.SentenceBreakingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentence-breaking")
public class SentenceBreakingController {

    private final SentenceBreakingService sentenceBreakingService;

    public SentenceBreakingController(SentenceBreakingService sentenceBreakingService) {
        this.sentenceBreakingService = sentenceBreakingService;
    }

    @GetMapping("/standards")
    public ApiResponse<SentenceBreakingStandardListDTO> getStandards() {
        SentenceBreakingStandardListDTO dto = sentenceBreakingService.getSentenceBreakingStandards();
        return ResponseUtil.success(dto);
    }

    @PostMapping("/settings")
    public ApiResponse<SentenceBreakingSettingResponseDTO> saveSetting(
            @RequestParam("taskid") Long taskId,
            @RequestBody SentenceBreakingSettingRequest request) {
        SentenceBreakingSettingResponseDTO dto = sentenceBreakingService.saveSentenceBreakingSetting(taskId, request);
        return ResponseUtil.success(dto);
    }
}

