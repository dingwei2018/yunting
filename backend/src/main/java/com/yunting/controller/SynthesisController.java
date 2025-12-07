package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.util.ValidationUtil;
import com.yunting.dto.synthesis.SynthesisBreakingSentenceRequest;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.service.RocketMQTtsCallbackService;
import com.yunting.service.SynthesisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     */
    @PostMapping("/breakingSentence")
    public ApiResponse<String> synthesizeBreakingSentence(
            @RequestBody SynthesisBreakingSentenceRequest request) {
        ValidationUtil.notNull(request.getBreakingSentenceId(), "breakingSentenceId不能为空");
        String status = synthesisService.synthesize(request.getBreakingSentenceId());
        return ResponseUtil.success(status);
    }

    /**
     * 设置拆句合成参数
     * 覆盖旧数据
     */
    @PostMapping("/setConfig")
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
}


