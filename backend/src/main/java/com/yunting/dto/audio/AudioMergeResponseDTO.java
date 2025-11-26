package com.yunting.dto.audio;

public class AudioMergeResponseDTO {
    private Long mergeId;
    private Long taskId;
    private String mergedAudioUrl;
    private Integer audioDuration;
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


