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

    /**
     * 等待所有进行中的TTS任务完成
     * 
     * @param maxWaitSeconds 最大等待时间（秒）
     * @param excludeBreakingSentenceId 要排除的断句ID（当前正在处理的断句，可为null）
     * @return 是否所有任务都已完成
     */
    boolean waitForProcessingTasks(int maxWaitSeconds, Long excludeBreakingSentenceId);
}
