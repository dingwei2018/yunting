package com.yunting.dto.voice;

import java.util.List;

/**
 * 语音配置列表响应DTO
 */
public class VoiceConfigListResponseDTO {
    /**
     * 语音配置列表
     */
    private List<VoiceConfigDTO> list;

    public List<VoiceConfigDTO> getList() {
        return list;
    }

    public void setList(List<VoiceConfigDTO> list) {
        this.list = list;
    }
}


