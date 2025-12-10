package com.yunting.dto.synthesis;

/**
 * 合成任务请求DTO
 */
public class SynthesisTaskRequest {
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

