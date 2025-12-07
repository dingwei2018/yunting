package com.yunting.service.impl;

import com.yunting.dto.voice.VoiceConfigDTO;
import com.yunting.dto.voice.VoiceConfigListResponseDTO;
import com.yunting.mapper.VoiceConfigMapper;
import com.yunting.model.VoiceConfig;
import com.yunting.service.VoiceConfigService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoiceConfigServiceImpl implements VoiceConfigService {

    private final VoiceConfigMapper voiceConfigMapper;

    public VoiceConfigServiceImpl(VoiceConfigMapper voiceConfigMapper) {
        this.voiceConfigMapper = voiceConfigMapper;
    }

    @Override
    public VoiceConfigListResponseDTO getVoiceConfigs() {
        List<VoiceConfig> configs = voiceConfigMapper.selectList(null, null);
        VoiceConfigListResponseDTO responseDTO = new VoiceConfigListResponseDTO();
        responseDTO.setList(configs.stream().map(this::toDTO).collect(Collectors.toList()));
        return responseDTO;
    }

    private VoiceConfigDTO toDTO(VoiceConfig config) {
        VoiceConfigDTO dto = new VoiceConfigDTO();
        dto.setVoiceId(config.getVoiceId());
        dto.setVoiceName(config.getVoiceName());
        dto.setVoiceType(config.getVoiceType());
        dto.setSortOrder(config.getSortOrder());
        dto.setHeaderUrl(config.getAvatarUrl());
        return dto;
    }
}


