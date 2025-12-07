package com.yunting.dto.original;

public class BreakingSentenceWithSettingDTO {
    private Long breakingSentenceId;
    private Long taskId;
    private Long originalSentenceId;
    private String content;
    private Integer charCount;
    private Integer sequence;
    private Integer synthesisStatus;
    private String ssml;
    private String jobId;
    private String createdAt;
    private String updatedAt;
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

