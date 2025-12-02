package com.yunting.dto.synthesis;

/**
 * TTS合成请求消息DTO
 * 用于在RocketMQ中传递合成请求
 */
public class TtsSynthesisRequest {
    private Long breakingSentenceId;
    private String voiceId;
    private Integer speechRate;
    private Integer volume;
    private Integer pitch;
    private boolean resetStatus;
    private String content;  // 断句内容
    
    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }
    
    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }
    
    public String getVoiceId() {
        return voiceId;
    }
    
    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
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
    
    public boolean isResetStatus() {
        return resetStatus;
    }
    
    public void setResetStatus(boolean resetStatus) {
        this.resetStatus = resetStatus;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}

