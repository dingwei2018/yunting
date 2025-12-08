package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.original.OriginalSentenceListResponseDTO;
import com.yunting.service.OriginalSentenceService;
import com.yunting.util.ValidationUtil;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/delete")
    public ApiResponse<String> deleteOriginalSentence(
            @RequestParam("originalSentenceId") Long originalSentenceId) {
        ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
        originalSentenceService.deleteOriginalSentence(originalSentenceId);
        return ResponseUtil.success("删除成功");
    }
}
