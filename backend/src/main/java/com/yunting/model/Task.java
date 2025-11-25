package com.yunting.model;

import java.time.LocalDateTime;

public class Task {
    private Long taskId;
    private String content;
    private Integer charCount;
    private Integer status;
    private String mergedAudioUrl;
    private Integer mergedAudioDuration;
    private Integer breakingStandardId;
    private Integer charCountLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMergedAudioUrl() {
        return mergedAudioUrl;
    }

    public void setMergedAudioUrl(String mergedAudioUrl) {
        this.mergedAudioUrl = mergedAudioUrl;
    }

    public Integer getMergedAudioDuration() {
        return mergedAudioDuration;
    }

    public void setMergedAudioDuration(Integer mergedAudioDuration) {
        this.mergedAudioDuration = mergedAudioDuration;
    }

    public Integer getBreakingStandardId() {
        return breakingStandardId;
    }

    public void setBreakingStandardId(Integer breakingStandardId) {
        this.breakingStandardId = breakingStandardId;
    }

    public Integer getCharCountLimit() {
        return charCountLimit;
    }

    public void setCharCountLimit(Integer charCountLimit) {
        this.charCountLimit = charCountLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

