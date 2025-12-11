package com.yunting.service;

import com.yunting.dto.synthesis.TtsSynthesisRequest;

/**
 * TTS合成协调器
 * 负责协调阅读规则更新和TTS合成
 */
public interface TtsSynthesisCoordinator {

    /**
     * 确保阅读规则已同步到华为云，然后执行合成
     * 
     * @param request TTS合成请求
     * @param createTtsJobCallback 创建TTS任务的回调函数
     */
    void ensureVocabularyConfigsAndSynthesize(TtsSynthesisRequest request, 
                                               Runnable createTtsJobCallback);
}
