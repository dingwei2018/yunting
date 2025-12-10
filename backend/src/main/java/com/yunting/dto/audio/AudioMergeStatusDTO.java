package com.yunting.dto.audio;

/**
 * 音频合并状态DTO
 */
public class AudioMergeStatusDTO {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 合并后的音频URL
     */
    private String mergedAudioUrl;
    /**
     * 音频时长（秒）
     */
    private Integer audioDuration;
    /**
     * 合并状态（0-未开始，1-合并中，2-已完成，3-失败）
     */
    private Integer status;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

