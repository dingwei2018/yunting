package com.yunting.dto.task;

import java.time.LocalDateTime;

/**
 * 原始拆句DTO
 */
public class OriginalSentenceDTO {
    /**
     * 拆句ID
     */
    private Long originalSentenceId;
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 拆句内容
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
     * 创建时间
     */
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

