package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.reading.MatchingFieldListResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleCreateResponseDTO;
import com.yunting.dto.reading.ReadingRuleDeleteRequest;
import com.yunting.dto.reading.ReadingRuleListPageResponseDTO;
import com.yunting.dto.reading.ReadingRuleSetGlobalSettingRequest;
import com.yunting.service.ReadingRuleService;
import com.yunting.util.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅读规范/阅读规范控制器
 * @module 阅读规范
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
     * 创建新的阅读规范规则
     *
     * @param request 创建阅读规范请求，包含 pattern、ruleType 和 ruleValue
     * @return 创建结果，包含 ruleId、任务ID、模式、规则类型、规则值、作用域和创建时间
     */
    @PostMapping("/create")
    public ApiResponse<ReadingRuleCreateResponseDTO> createReadingRule(@Valid @RequestBody ReadingRuleCreateRequest request) {
        ReadingRuleCreateResponseDTO data = readingRuleService.createReadingRule(request);
        return ResponseUtil.success(data);
    }

    /**
     * 开关全局阅读规范
     * 设置全局阅读规范的开关状态
     *
     * @param request 开关全局阅读规范请求，包含 taskId、ruleId、isOpen 和 breakingSentenceId
     * @return 设置结果，成功返回设置成功的消息
     */
    @PostMapping("/setGlobalSetting")
    public ApiResponse<String> setGlobalSetting(@Valid @RequestBody ReadingRuleSetGlobalSettingRequest request) {
        String data = readingRuleService.setGlobalSetting(request);
        return ResponseUtil.success(data);
    }

    /**
     * 获取阅读规范列表（支持分页和筛选）
     * 根据任务ID和规则类型筛选阅读规范列表
     *
     * @param taskId 任务ID（可选）
     * @param ruleType 规则类型（可选，1-数字英文，2-音标调整，3-专有词汇）
     * @param page 页码（必填）
     * @param pageSize 每页大小（必填）
     * @return 阅读规范列表响应，包含阅读规范列表、总数、页码和每页大小
     */
    @GetMapping("/getList")
    public ApiResponse<ReadingRuleListPageResponseDTO> getReadingRuleList(
            @RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "ruleType", required = false) Integer ruleType,
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize) {
        ReadingRuleListPageResponseDTO data = readingRuleService.getReadingRuleList(taskId, ruleType, page, pageSize);
        return ResponseUtil.success(data);
    }

    /**
     * 获取文本中符合规则的字段列表
     * 从文本中提取符合阅读规则的字段列表
     *
     * @param taskId 任务ID（必选）
     * @param breakingSentenceId 断句ID（可选）
     * @param content 文本内容（必选）
     * @return 匹配的字段列表响应，包含匹配字段总数和字段列表（包含规则ID、位置、模式和开关状态）
     */
    @GetMapping("/getMatchingFieldListFromText")
    public ApiResponse<MatchingFieldListResponseDTO> getMatchingFieldListFromText(
            @RequestParam(value = "taskId") Long taskId,
            @RequestParam(value = "breakingSentenceId", required = false) Long breakingSentenceId,
            @RequestParam(value = "content") String content) {
        MatchingFieldListResponseDTO data = readingRuleService.getMatchingFieldListFromText(taskId, breakingSentenceId, content);
        return ResponseUtil.success(data);
    }

    /**
     * 删除全局阅读规则
     * 删除指定的阅读规则及其关联的应用记录
     *
     * @param request 删除请求，包含 ruleId
     * @return 删除结果，成功返回"删除成功"
     */
    @PostMapping("/delete")
    public ApiResponse<String> deleteReadingRule(
            @RequestBody ReadingRuleDeleteRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        ValidationUtil.notNull(request.getRuleId(), "ruleId不能为空");
        readingRuleService.deleteReadingRule(request.getRuleId());
        return ResponseUtil.success("删除成功");
    }
}
