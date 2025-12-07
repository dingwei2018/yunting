package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.synthesis.BreakingSentenceSynthesisResponseDTO;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.TaskSynthesisBatchResponseDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.service.RocketMQTtsCallbackService;
import com.yunting.service.SynthesisService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SynthesisController {

    private final SynthesisService synthesisService;
    private final RocketMQTtsCallbackService rocketMQTtsCallbackService;

    public SynthesisController(SynthesisService synthesisService,
                                RocketMQTtsCallbackService rocketMQTtsCallbackService) {
        this.synthesisService = synthesisService;
        this.rocketMQTtsCallbackService = rocketMQTtsCallbackService;
    }

    /**
     * 语音合成接口
     */
    @PostMapping("/breaking-sentences/synthesize")
    public ApiResponse<BreakingSentenceSynthesisResponseDTO> synthesize(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId,
            @RequestParam(value = "voice_id") String voiceId,
            @RequestParam(value = "speech_rate", required = false) Integer speechRate,
            @RequestParam(value = "volume", required = false) Integer volume,
            @RequestParam(value = "pitch", required = false) Integer pitch) {
        BreakingSentenceSynthesisResponseDTO data = synthesisService.synthesize(
                breakingSentenceId, voiceId, speechRate, volume, pitch, false);
        return ResponseUtil.success(data);
    }

    @PostMapping("/breaking-sentences/resynthesize")
    public ApiResponse<BreakingSentenceSynthesisResponseDTO> reSynthesize(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId,
            @RequestParam(value = "voice_id", required = false) String voiceId,
            @RequestParam(value = "speech_rate", required = false) Integer speechRate,
            @RequestParam(value = "volume", required = false) Integer volume,
            @RequestParam(value = "pitch", required = false) Integer pitch) {
        BreakingSentenceSynthesisResponseDTO data = synthesisService.synthesize(
                breakingSentenceId, voiceId, speechRate, volume, pitch, true);
        return ResponseUtil.success(data);
    }

    @PostMapping("/tasks/synthesize")
    public ApiResponse<TaskSynthesisBatchResponseDTO> synthesizeTask(
            @RequestParam("taskid") Long taskId,
            @RequestParam(value = "voice_id", required = false) String voiceId,
            @RequestParam(value = "speech_rate", required = false) Integer speechRate,
            @RequestParam(value = "volume", required = false) Integer volume,
            @RequestParam(value = "pitch", required = false) Integer pitch,
            @RequestParam(value = "breaking_sentence_ids", required = false) String breakingSentenceIds) {
        List<Long> ids = parseIds(breakingSentenceIds);
        TaskSynthesisBatchResponseDTO data = synthesisService.synthesizeBatch(taskId, voiceId, speechRate, volume, pitch, ids);
        return ResponseUtil.success(data);
    }

    @GetMapping("/synthesis/tasks")
    public ApiResponse<TaskSynthesisStatusDTO> getSynthesisStatus(@RequestParam("taskid") Long taskId) {
        TaskSynthesisStatusDTO dto = synthesisService.getTaskSynthesisStatus(taskId);
        return ResponseUtil.success(dto);
    }

    /**
     * 设置拆句合成参数
     * 覆盖旧数据
     */
    @PostMapping("/synthesis/setConfig")
    public ApiResponse<String> setConfig(@RequestBody SynthesisSetConfigRequest request) {
        synthesisService.setConfig(request);
        return ResponseUtil.success("配置更新成功");
    }

    /**
     * 华为云TTS回调接口
     * 接收华为云TTS异步任务的回调通知
     * 将回调请求发送到RocketMQ消息队列，由消费者异步处理
     * 
     * @param callbackRequest 回调请求体，包含任务状态、job_id、音频下载URL等信息
     * @return 处理结果
     */
    @PostMapping("/synthesis/callback")
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

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}


