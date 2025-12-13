package com.yunting.dto.synthesis;

/**
 * 取消任务合成请求DTO
 */
public class CancelTaskRequest {
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

