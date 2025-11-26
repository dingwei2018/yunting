package com.yunting.dto.breaking;

public class BreakingSentenceSettingsDTO {
    private String voiceId;
    private String voiceName;
    private Integer speechRate;
    private Integer volume;
    private Integer pitch;

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
}


