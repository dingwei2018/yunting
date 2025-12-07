package com.yunting.dto.audio;

/**
 * 音频合并消息
 * 用于在 RocketMQ 中传递合并请求
 */
public class AudioMergeMessage {
    private Long taskId;
    private Long mergeId;
    
    public AudioMergeMessage() {
    }
    
    public AudioMergeMessage(Long taskId, Long mergeId) {
        this.taskId = taskId;
        this.mergeId = mergeId;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public Long getMergeId() {
        return mergeId;
    }
    
    public void setMergeId(Long mergeId) {
        this.mergeId = mergeId;
    }
}

