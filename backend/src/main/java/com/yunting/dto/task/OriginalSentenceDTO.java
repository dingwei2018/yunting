package com.yunting.dto.task;

import java.time.LocalDateTime;

/**
 * 原始拆句DTO
 */
public class OriginalSentenceDTO {
    private Long originalSentenceId;
    private Long taskId;
    private String content;
    private Integer charCount;
    private Integer sequence;
    private LocalDateTime createdAt;

    public Long getOriginalSentenceId() {
        return originalSentenceId;
    }

    public void setOriginalSentenceId(Long originalSentenceId) {
        this.originalSentenceId = originalSentenceId;
    }

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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

