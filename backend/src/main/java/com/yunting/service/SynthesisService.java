package com.yunting.service;

import com.yunting.dto.synthesis.SynthesisSetConfigRequest;

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
}


