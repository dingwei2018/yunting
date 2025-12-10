package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.util.ValidationUtil;
import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisBreakingSentenceRequest;
import com.yunting.dto.synthesis.SynthesisOriginalSentenceRequest;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.SynthesisTaskRequest;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.service.RocketMQTtsCallbackService;
import com.yunting.service.SynthesisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合成管理/合成管理控制器
 * @module 合成管理
 */
@RestController
@RequestMapping("/api/synthesis")
public class SynthesisController {

    private final SynthesisService synthesisService;
    private final RocketMQTtsCallbackService rocketMQTtsCallbackService;

    public SynthesisController(SynthesisService synthesisService,
                                RocketMQTtsCallbackService rocketMQTtsCallbackService) {
        this.synthesisService = synthesisService;
        this.rocketMQTtsCallbackService = rocketMQTtsCallbackService;
    }

    /**
     * 合成断句
     * 合成或重新合成单个断句
     *
     * @param request 合成请求，包含 breakingSentenceId
     * @return 合成状态，成功返回"合成请求已提交"
     */
    @PostMapping("/breakingSentence")
    public ApiResponse<String> synthesizeBreakingSentence(
            @RequestBody SynthesisBreakingSentenceRequest request) {
        ValidationUtil.notNull(request.getBreakingSentenceId(), "breakingSentenceId不能为空");
        String status = synthesisService.synthesize(request.getBreakingSentenceId());
        return ResponseUtil.success(status);
    }

    /**
     * 合成拆句
     * 合成或重新合成拆句下的所有断句
     *
     * @param request 合成请求，包含 originalSentenceId
     * @return 合成状态，成功返回"合成请求已提交"
     */
    @PostMapping("/originalSentence")
    public ApiResponse<String> synthesizeOriginalSentence(
            @RequestBody SynthesisOriginalSentenceRequest request) {
        ValidationUtil.notNull(request.getOriginalSentenceId(), "originalSentenceId不能为空");
        String status = synthesisService.synthesizeOriginalSentence(request.getOriginalSentenceId());
        return ResponseUtil.success(status);
    }

    /**
     * 合成任务
     * 合成或重新合成任务下的所有断句
     *
     * @param request 合成请求，包含 taskId
     * @return 合成状态，成功返回"合成请求已提交"
     */
    @PostMapping("/task")
    public ApiResponse<String> synthesizeTask(
            @RequestBody SynthesisTaskRequest request) {
        ValidationUtil.notNull(request.getTaskId(), "taskId不能为空");
        String status = synthesisService.synthesizeTask(request.getTaskId());
        return ResponseUtil.success(status);
    }

    /**
     * 设置拆句合成参数
     * 覆盖旧数据
     *
     * @param request 配置请求，包含任务ID、拆句ID和断句配置列表
     * @return 配置结果，成功返回"配置更新成功"
     */
    @PostMapping("/setConfig")
    public ApiResponse<String> setConfig(@RequestBody SynthesisSetConfigRequest request) {
        synthesisService.setConfig(request);
        return ResponseUtil.success("配置更新成功");
    }

    /**
     * @ignore
     *
     * 华为云TTS回调接口
     * 接收华为云TTS异步任务的回调通知
     * 将回调请求发送到RocketMQ消息队列，由消费者异步处理
     * 
     * @param callbackRequest 回调请求体，包含任务状态、job_id、音频下载URL等信息
     * @return 处理结果
     */
    @PostMapping("/callback")
    public ApiResponse<String> handleTtsCallback(@RequestBody TtsCallbackRequest callbackRequest) {
        try {
            // 发送消息到RocketMQ，而不是直接处理
            boolean success = rocketMQTtsCallbackService.sendTtsCallbackMessage(callbackRequest);
            if (success) {
                return ResponseUtil.success("回调消息已发送到消息队列");
            } else {
                return ResponseUtil.error(10500, "回调消息发送失败");
            }
        } catch (Exception e) {
            return ResponseUtil.error(10500, "回调处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取断句合成状态
     * 给出单句断句的合成状态和已完成合成的音频文件下载地址和时长
     * 
     * @param breakingSentenceId 断句ID
     * @return 合成结果，包含音频URL、时长和合成状态
     */
    @GetMapping("/getBreakingSentenceStatus")
    public ApiResponse<SynthesisResultDTO> getBreakingSentenceStatus(
            @RequestParam(required = false) Long breakingSentenceId) {
        ValidationUtil.notNull(breakingSentenceId, "breakingSentenceId不能为空");
        SynthesisResultDTO result = synthesisService.getBreakingSentenceStatus(breakingSentenceId);
        return ResponseUtil.success(result);
    }

    /**
     * 获取拆句合成状态
     * 给出拆句下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param originalSentenceId 拆句ID
     * @return 拆句合成状态，包含进度、统计信息和音频URL列表
     */
    @GetMapping("/getOriginalSentenceStatus")
    public ApiResponse<OriginalSentenceSynthesisStatusDTO> getOriginalSentenceStatus(
            @RequestParam(required = false) Long originalSentenceId) {
        ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
        OriginalSentenceSynthesisStatusDTO result = synthesisService.getOriginalSentenceStatus(originalSentenceId);
        return ResponseUtil.success(result);
    }

    /**
     * 获取任务合成状态
     * 给出任务下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param taskid 任务ID
     * @return 任务合成状态，包含进度、统计信息和音频URL列表
     */
    @GetMapping("/getTaskStatus")
    public ApiResponse<TaskSynthesisStatusDTO> getTaskStatus(
            @RequestParam(required = true) Long taskid) {
        ValidationUtil.notNull(taskid, "taskid不能为空");
        TaskSynthesisStatusDTO result = synthesisService.getTaskStatus(taskid);
        return ResponseUtil.success(result);
    }
}


