package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.voice.VoiceConfigListResponseDTO;
import com.yunting.service.VoiceConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice")
public class VoiceConfigController {

    private final VoiceConfigService voiceConfigService;

    public VoiceConfigController(VoiceConfigService voiceConfigService) {
        this.voiceConfigService = voiceConfigService;
    }

    @GetMapping("/getList")
    public ApiResponse<VoiceConfigListResponseDTO> getVoices() {
        VoiceConfigListResponseDTO dto = voiceConfigService.getVoiceConfigs();
        return ResponseUtil.success(dto);
    }
}


