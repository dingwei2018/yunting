package com.yunting.dto.synthesis;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 华为云TTS回调请求DTO
 * 用于接收华为云TTS异步任务的回调通知
 */
public class TtsCallbackRequest {

    /**
     * 任务状态
     * FINISHED - 已完成
     * ERROR - 失败
     * WAITING - 等待中
     */
    @JsonProperty("status")
    private String status;

    /**
     * 任务ID（华为云返回的job_id）
     */
    @JsonProperty("job_id")
    private String jobId;

    /**
     * 音频文件下载URL
     */
    @JsonProperty("audio_file_download_url")
    private String audioFileDownloadUrl;

    /**
     * 字幕文件下载URL（可选）
     */
    @JsonProperty("subtitle_file_download_url")
    private String subtitleFileDownloadUrl;

    /**
     * 音频时长（秒）
     */
    @JsonProperty("audio_duration")
    private Integer audioDuration;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getAudioFileDownloadUrl() {
        return audioFileDownloadUrl;
    }

    public void setAudioFileDownloadUrl(String audioFileDownloadUrl) {
        this.audioFileDownloadUrl = audioFileDownloadUrl;
    }

    public String getSubtitleFileDownloadUrl() {
        return subtitleFileDownloadUrl;
    }

    public void setSubtitleFileDownloadUrl(String subtitleFileDownloadUrl) {
        this.subtitleFileDownloadUrl = subtitleFileDownloadUrl;
    }

    public Integer getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }
}

