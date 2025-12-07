package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.original.OriginalSentenceListResponseDTO;
import com.yunting.service.OriginalSentenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/originalSentence")
public class OriginalSentenceController {

    private final OriginalSentenceService originalSentenceService;

    public OriginalSentenceController(OriginalSentenceService originalSentenceService) {
        this.originalSentenceService = originalSentenceService;
    }

    @GetMapping("/getOriginalSentenceList")
    public ApiResponse<OriginalSentenceListResponseDTO> getOriginalSentenceList(
            @RequestParam("taskid") Long taskId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize) {
        OriginalSentenceListResponseDTO data = originalSentenceService.getOriginalSentenceList(taskId, page, pageSize);
        return ResponseUtil.success(data);
    }
}
