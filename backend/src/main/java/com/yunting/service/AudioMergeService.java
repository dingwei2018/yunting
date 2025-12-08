package com.yunting.service;

import com.yunting.dto.audio.AudioMergeMessage;
import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.dto.audio.AudioMergeStatusDTO;

public interface AudioMergeService {

    /**
     * 创建合并任务并发送到消息队列
     * 
     * @param taskId 任务ID
     * @param request 合并请求
     * @return 合并响应（包含merge_id，状态为processing）
     */
    AudioMergeResponseDTO mergeAudio(Long taskId, AudioMergeRequest request);
    
    /**
     * 处理音频合并（由消费者调用）
     * 
     * @param mergeMessage 合并消息
     */
    void processAudioMerge(AudioMergeMessage mergeMessage);
    
    /**
     * 获取合并状态
     * 
     * @param mergeId 合并ID
     * @return 合并状态信息
     */
    AudioMergeStatusDTO getMergeStatus(Long mergeId);
}


