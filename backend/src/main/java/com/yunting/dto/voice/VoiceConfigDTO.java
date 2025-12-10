package com.yunting.dto.voice;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 语音配置DTO
 */
public class VoiceConfigDTO {
    /**
     * 语音ID
     */
    private String voiceId;
    /**
     * 语音名称
     */
    private String voiceName;
    /**
     * 语音类型
     */
    private String voiceType;
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    /**
     * 头像URL
     */
    @JsonProperty("avatar_url")
    private String headerUrl;

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceType() {
        return voiceType;
    }

    public void setVoiceType(String voiceType) {
        this.voiceType = voiceType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getHeaderUrl() {
        return headerUrl;
    }

    public void setHeaderUrl(String headerUrl) {
        this.headerUrl = headerUrl;
    }
}


