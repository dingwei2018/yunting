package com.yunting.dto.synthesis;

/**
 * 合成结果DTO
 */
public class SynthesisResultDTO {
    /**
     * 音频URL
     */
    private String audioUrl;
    /**
     * 音频时长（秒）
     */
    private Integer audioDuration;
    /**
     * 合成状态（0-未合成，1-合成中，2-已合成，3-合成失败）
     */
    private Integer synthesisStatus;

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public Integer getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }

    public Integer getSynthesisStatus() {
        return synthesisStatus;
    }

    public void setSynthesisStatus(Integer synthesisStatus) {
        this.synthesisStatus = synthesisStatus;
    }
}


