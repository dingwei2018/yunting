package com.yunting.dto.original;

/**
 * 带配置的断句DTO
 */
public class BreakingSentenceWithSettingDTO {
    /**
     * 断句ID
     */
    private Long breakingSentenceId;
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 拆句ID
     */
    private Long originalSentenceId;
    /**
     * 断句内容
     */
    private String content;
    /**
     * 字符数
     */
    private Integer charCount;
    /**
     * 序号
     */
    private Integer sequence;
    /**
     * 合成状态（0-未合成，1-合成中，2-已合成，3-合成失败）
     */
    private Integer synthesisStatus;
    /**
     * 音频URL
     */
    private String audioUrl;
    /**
     * 音频时长（秒）
     */
    private Integer audioDuration;
    /**
     * SSML内容
     */
    private String ssml;
    /**
     * 华为云任务ID
     */
    private String jobId;
    /**
     * 创建时间
     */
    private String createdAt;
    /**
     * 更新时间
     */
    private String updatedAt;
    /**
     * 断句配置
     */
    private BreakingSentenceSettingDTO setting;

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getOriginalSentenceId() {
        return originalSentenceId;
    }

    public void setOriginalSentenceId(Long originalSentenceId) {
        this.originalSentenceId = originalSentenceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getSynthesisStatus() {
        return synthesisStatus;
    }

    public void setSynthesisStatus(Integer synthesisStatus) {
        this.synthesisStatus = synthesisStatus;
    }

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

    public String getSsml() {
        return ssml;
    }

    public void setSsml(String ssml) {
        this.ssml = ssml;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BreakingSentenceSettingDTO getSetting() {
        return setting;
    }

    public void setSetting(BreakingSentenceSettingDTO setting) {
        this.setting = setting;
    }
}

