package com.yunting.service;

import com.yunting.dto.synthesis.BreakingSentenceSynthesisResponseDTO;
import com.yunting.dto.synthesis.TaskSynthesisBatchResponseDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;

import java.util.List;

public interface SynthesisService {

    BreakingSentenceSynthesisResponseDTO synthesize(Long breakingSentenceId,
                                                    String voiceId,
                                                    Integer speechRate,
                                                    Integer volume,
                                                    Integer pitch,
                                                    boolean resetStatus);

    TaskSynthesisBatchResponseDTO synthesizeBatch(Long taskId,
                                                  String voiceId,
                                                  Integer speechRate,
                                                  Integer volume,
                                                  Integer pitch,
                                                  List<Long> breakingSentenceIds);

    TaskSynthesisStatusDTO getTaskSynthesisStatus(Long taskId);

    /**
     * 处理华为云TTS回调
     * 
     * @param callbackRequest 回调请求
     */
    void handleTtsCallback(com.yunting.dto.synthesis.TtsCallbackRequest callbackRequest);
}


