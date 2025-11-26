package com.yunting.dto.breaking.request;

import java.util.List;

public class BreakingSentenceParamRequest {
    private Long breakingSentenceId;
    private String content;
    private Integer speechRate;
    private Integer volume;
    private Integer pitch;
    private String voiceId;
    private List<PauseSettingRequest> pauses;
    private List<PolyphonicSettingRequest> polyphonic;
    private List<Long> readingRuleIds;

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(Integer speechRate) {
        this.speechRate = speechRate;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getPitch() {
        return pitch;
    }

    public void setPitch(Integer pitch) {
        this.pitch = pitch;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public List<PauseSettingRequest> getPauses() {
        return pauses;
    }

    public void setPauses(List<PauseSettingRequest> pauses) {
        this.pauses = pauses;
    }

    public List<PolyphonicSettingRequest> getPolyphonic() {
        return polyphonic;
    }

    public void setPolyphonic(List<PolyphonicSettingRequest> polyphonic) {
        this.polyphonic = polyphonic;
    }

    public List<Long> getReadingRuleIds() {
        return readingRuleIds;
    }

    public void setReadingRuleIds(List<Long> readingRuleIds) {
        this.readingRuleIds = readingRuleIds;
    }
}


