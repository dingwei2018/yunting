package com.yunting.service.impl;

import com.yunting.dto.voice.VoiceConfigDTO;
import com.yunting.dto.voice.VoiceConfigListResponseDTO;
import com.yunting.mapper.VoiceConfigMapper;
import com.yunting.model.VoiceConfig;
import com.yunting.service.VoiceConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoiceConfigServiceImpl implements VoiceConfigService {

    private final VoiceConfigMapper voiceConfigMapper;

    public VoiceConfigServiceImpl(VoiceConfigMapper voiceConfigMapper) {
        this.voiceConfigMapper = voiceConfigMapper;
    }

    @Override
    public VoiceConfigListResponseDTO getVoiceConfigs(Integer isRecommended, String language) {
        List<VoiceConfig> configs = voiceConfigMapper.selectList(isRecommended, StringUtils.hasText(language) ? language : null);
        VoiceConfigListResponseDTO responseDTO = new VoiceConfigListResponseDTO();
        responseDTO.setList(configs.stream().map(this::toDTO).collect(Collectors.toList()));
        return responseDTO;
    }

    private VoiceConfigDTO toDTO(VoiceConfig config) {
        VoiceConfigDTO dto = new VoiceConfigDTO();
        dto.setVoiceId(config.getVoiceId());
        dto.setVoiceName(config.getVoiceName());
        dto.setVoiceType(config.getVoiceType());
        dto.setLanguage(config.getLanguage());
        dto.setIsRecommended(config.getIsRecommended());
        dto.setSortOrder(config.getSortOrder());
        return dto;
    }
}


