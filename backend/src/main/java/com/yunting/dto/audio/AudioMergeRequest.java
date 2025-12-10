package com.yunting.dto.audio;

/**
 * 音频合并请求DTO
 */
public class AudioMergeRequest {
    /**
     * 任务ID（必填）
     */
    private Long taskId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}


