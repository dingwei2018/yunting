package com.yunting.model;

import java.time.LocalDateTime;

public class AudioMerge {
    private Long mergeId;
    private Long taskId;
    private String breakingSentenceIds;
    private String mergedAudioUrl;
    private Integer audioDuration;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getMergeId() {
        return mergeId;
    }

    public void setMergeId(Long mergeId) {
        this.mergeId = mergeId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getBreakingSentenceIds() {
        return breakingSentenceIds;
    }

    public void setBreakingSentenceIds(String breakingSentenceIds) {
        this.breakingSentenceIds = breakingSentenceIds;
    }

    public String getMergedAudioUrl() {
        return mergedAudioUrl;
    }

    public void setMergedAudioUrl(String mergedAudioUrl) {
        this.mergedAudioUrl = mergedAudioUrl;
    }

    public Integer getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(Integer audioDuration) {
        this.audioDuration = audioDuration;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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


