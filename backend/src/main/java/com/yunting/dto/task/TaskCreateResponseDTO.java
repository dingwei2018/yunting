package com.yunting.dto.task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务响应DTO
 */
public class TaskCreateResponseDTO {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 任务文本内容
     */
    private String content;
    /**
     * 字符数
     */
    private Integer charCount;
    /**
     * 任务状态
     */
    private Integer status;
    /**
     * 音频URL
     */
    private String audioUrl;
    /**
     * 音频时长（秒）
     */
    private Integer audioDuration;
    /**
     * 拆句列表
     */
    private List<OriginalSentenceDTO> originalSentenceList;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
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

