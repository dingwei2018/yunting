package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.voice.VoiceConfigListResponseDTO;
import com.yunting.service.VoiceConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 语音配置/语音配置控制器
 * @module 语音配置
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceConfigController {

    private final VoiceConfigService voiceConfigService;

    public VoiceConfigController(VoiceConfigService voiceConfigService) {
        this.voiceConfigService = voiceConfigService;
    }

    /**
     * 获取语音列表
     * 获取所有可用的语音配置列表
     *
     * @return 语音配置列表响应，包含所有可用的语音配置（语音ID、名称、类型、排序和头像URL）
     */
    @GetMapping("/getList")
    public ApiResponse<VoiceConfigListResponseDTO> getVoices() {
        VoiceConfigListResponseDTO dto = voiceConfigService.getVoiceConfigs();
        return ResponseUtil.success(dto);
    }
}


