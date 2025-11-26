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
}


