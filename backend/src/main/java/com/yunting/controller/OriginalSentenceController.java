package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.original.OriginalSentenceDeleteRequest;
import com.yunting.dto.original.OriginalSentenceListResponseDTO;
import com.yunting.service.OriginalSentenceService;
import com.yunting.util.ValidationUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 拆句管理/拆句管理控制器
 * @module 拆句管理
 */
@RestController
@RequestMapping("/api/originalSentence")
public class OriginalSentenceController {

    private final OriginalSentenceService originalSentenceService;

    public OriginalSentenceController(OriginalSentenceService originalSentenceService) {
        this.originalSentenceService = originalSentenceService;
    }

    /**
     * 获取拆句列表
     * 获取指定任务下的拆句列表，支持分页
     *
     * @param taskId 任务ID（必填）
     * @param page 页码（可选，默认为1）
     * @param pageSize 每页大小（可选，默认为10）
     * @return 拆句列表响应，包含列表数据、总数、页码和每页大小
     */
    @GetMapping("/getOriginalSentenceList")
    public ApiResponse<OriginalSentenceListResponseDTO> getOriginalSentenceList(
            @RequestParam("taskid") Long taskId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize) {
        OriginalSentenceListResponseDTO data = originalSentenceService.getOriginalSentenceList(taskId, page, pageSize);
        return ResponseUtil.success(data);
    }

    /**
     * 删除拆句
     * 删除指定的拆句及其关联的断句
     *
     * @param request 删除请求，包含 originalSentenceId
     * @return 删除结果，成功返回"删除成功"
     */
    @PostMapping("/delete")
    public ApiResponse<String> deleteOriginalSentence(
            @RequestBody OriginalSentenceDeleteRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        ValidationUtil.notNull(request.getOriginalSentenceId(), "originalSentenceId不能为空");
        originalSentenceService.deleteOriginalSentence(request.getOriginalSentenceId());
        return ResponseUtil.success("删除成功");
    }
}
