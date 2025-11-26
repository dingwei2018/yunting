package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;
import com.yunting.service.BreakingSentenceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BreakingSentenceController {

    private final BreakingSentenceService breakingSentenceService;

    public BreakingSentenceController(BreakingSentenceService breakingSentenceService) {
        this.breakingSentenceService = breakingSentenceService;
    }

    @GetMapping("/tasks/breaking-sentences")
    public ApiResponse<BreakingSentenceListResponseDTO> getBreakingSentenceList(
            @RequestParam("taskid") Long taskId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize) {
        BreakingSentenceListResponseDTO data = breakingSentenceService.getBreakingSentenceList(taskId, page, pageSize);
        return ResponseUtil.success(data);
    }

    @GetMapping("/breaking-sentences/info")
    public ApiResponse<BreakingSentenceDetailDTO> getBreakingSentenceDetail(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId) {
        BreakingSentenceDetailDTO detail = breakingSentenceService.getBreakingSentenceDetail(breakingSentenceId);
        return ResponseUtil.success(detail);
    }

    @DeleteMapping("/breaking-sentences")
    public ApiResponse<Map<String, Long>> deleteBreakingSentence(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId) {
        breakingSentenceService.deleteBreakingSentence(breakingSentenceId);
        return ResponseUtil.success(Map.of("breaking_sentence_id", breakingSentenceId));
    }
}


