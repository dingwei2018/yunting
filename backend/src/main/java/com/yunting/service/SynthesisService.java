package com.yunting.service;

import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.SynthesisResultDTO;

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
}


