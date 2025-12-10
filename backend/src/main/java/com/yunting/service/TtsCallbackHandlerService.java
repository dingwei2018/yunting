package com.yunting.service;

import com.yunting.dto.synthesis.TtsCallbackRequest;

/**
 * TTS回调处理服务
 * 负责处理华为云TTS回调通知
 */
public interface TtsCallbackHandlerService {

    /**
     * 处理华为云TTS回调
     * 
     * @param callbackRequest 回调请求
     */
    void handleTtsCallback(TtsCallbackRequest callbackRequest);
}
