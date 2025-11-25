package com.yunting.dto.task;

import java.time.LocalDateTime;
import java.util.List;

public class TaskDetailDTO {
    private Long taskId;
    private String content;
    private Integer charCount;
    private Integer status;
    private String mergedAudioUrl;
    private Integer mergedAudioDuration;
    private String ssml;
    private Integer totalSentences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TaskSentenceDTO> sentences;
    private List<BreakingSentenceDTO> breakingSentences;

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

    public String getSsml() {
        return ssml;
    }

    public void setSsml(String ssml) {
        this.ssml = ssml;
    }

    public Integer getTotalSentences() {
        return totalSentences;
    }

    public void setTotalSentences(Integer totalSentences) {
        this.totalSentences = totalSentences;
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

    public List<TaskSentenceDTO> getSentences() {
        return sentences;
    }

    public void setSentences(List<TaskSentenceDTO> sentences) {
        this.sentences = sentences;
    }

    public List<BreakingSentenceDTO> getBreakingSentences() {
        return breakingSentences;
    }

    public void setBreakingSentences(List<BreakingSentenceDTO> breakingSentences) {
        this.breakingSentences = breakingSentences;
    }
}

