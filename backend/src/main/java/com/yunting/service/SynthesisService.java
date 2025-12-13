package com.yunting.service;

import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;

public interface SynthesisService {

    String synthesize(Long breakingSentenceId);

    String synthesizeOriginalSentence(Long originalSentenceId);

    String synthesizeTask(Long taskId);

    /**
     * 设置拆句合成参数
     * 
     * @param request 配置请求
     */
    void setConfig(SynthesisSetConfigRequest request);

    /**
     * 处理华为云TTS回调
     * 
     * @param callbackRequest 回调请求
     */
    void handleTtsCallback(com.yunting.dto.synthesis.TtsCallbackRequest callbackRequest);

    /**
     * 获取断句合成状态
     * 给出单句断句的合成状态和已完成合成的音频文件下载地址和时长
     * 
     * @param breakingSentenceId 断句ID
     * @return 合成结果DTO，包含音频URL、时长和合成状态
     */
    SynthesisResultDTO getBreakingSentenceStatus(Long breakingSentenceId);

    /**
     * 获取拆句合成状态
     * 给出拆句下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param originalSentenceId 拆句ID
     * @return 拆句合成状态DTO，包含进度、统计信息和音频URL列表
     */
    OriginalSentenceSynthesisStatusDTO getOriginalSentenceStatus(Long originalSentenceId);

    /**
     * 获取任务合成状态
     * 给出任务下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param taskId 任务ID
     * @return 任务合成状态DTO，包含进度、统计信息和音频URL列表
     */
    TaskSynthesisStatusDTO getTaskStatus(Long taskId);

    /**
     * 取消断句合成任务
     * 只能取消PROCESSING状态的合成任务，取消后将状态重置为PENDING并清空jobId
     * 
     * @param breakingSentenceId 断句ID
     * @return 取消结果消息
     */
    String cancelSynthesis(Long breakingSentenceId);

    /**
     * 取消拆句合成任务
     * 取消拆句下所有PROCESSING状态的断句合成任务
     * 
     * @param originalSentenceId 拆句ID
     * @return 取消结果消息，包含成功和失败的数量统计
     */
    String cancelOriginalSentence(Long originalSentenceId);

    /**
     * 取消任务合成任务
     * 取消任务下所有PROCESSING状态的断句合成任务
     * 
     * @param taskId 任务ID
     * @return 取消结果消息，包含成功和失败的数量统计
     */
    String cancelTask(Long taskId);
}


