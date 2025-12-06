package com.yunting.dto.task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务响应DTO
 */
public class TaskCreateResponseDTO {
    private Long taskId;
    private String content;
    private Integer charCount;
    private Integer status;
    private String audioUrl;
    private Integer audioDuration;
    private List<OriginalSentenceDTO> originalSentenceList;
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

    public List<OriginalSentenceDTO> getOriginalSentenceList() {
        return originalSentenceList;
    }

    public void setOriginalSentenceList(List<OriginalSentenceDTO> originalSentenceList) {
        this.originalSentenceList = originalSentenceList;
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

