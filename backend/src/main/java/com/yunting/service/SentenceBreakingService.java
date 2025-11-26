package com.yunting.service;

import com.yunting.dto.standard.SentenceBreakingSettingRequest;
import com.yunting.dto.standard.SentenceBreakingSettingResponseDTO;
import com.yunting.dto.standard.SentenceBreakingStandardListDTO;

public interface SentenceBreakingService {

    SentenceBreakingStandardListDTO getSentenceBreakingStandards();

    SentenceBreakingSettingResponseDTO saveSentenceBreakingSetting(Long taskId, SentenceBreakingSettingRequest request);
}


