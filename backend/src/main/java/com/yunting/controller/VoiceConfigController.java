package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.voice.VoiceConfigListResponseDTO;
import com.yunting.service.VoiceConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voices")
public class VoiceConfigController {

    private final VoiceConfigService voiceConfigService;

    public VoiceConfigController(VoiceConfigService voiceConfigService) {
        this.voiceConfigService = voiceConfigService;
    }

    @GetMapping
    public ApiResponse<VoiceConfigListResponseDTO> getVoices(
            @RequestParam(value = "is_recommended", required = false) Integer isRecommended,
            @RequestParam(value = "language", required = false) String language) {
        VoiceConfigListResponseDTO dto = voiceConfigService.getVoiceConfigs(isRecommended, language);
        return ResponseUtil.success(dto);
    }
}


