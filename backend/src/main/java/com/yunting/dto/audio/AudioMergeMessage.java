package com.yunting.dto.audio;

import java.util.List;

/**
 * 音频合并消息
 * 用于在 RocketMQ 中传递合并请求
 */
public class AudioMergeMessage {
    private Long taskId;
    private Long mergeId;
    private List<Long> sentenceIds;
    
    public AudioMergeMessage() {
    }
    
    public AudioMergeMessage(Long taskId, Long mergeId, List<Long> sentenceIds) {
        this.taskId = taskId;
        this.mergeId = mergeId;
        this.sentenceIds = sentenceIds;
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
    
    public List<Long> getSentenceIds() {
        return sentenceIds;
    }
    
    public void setSentenceIds(List<Long> sentenceIds) {
        this.sentenceIds = sentenceIds;
    }
}

