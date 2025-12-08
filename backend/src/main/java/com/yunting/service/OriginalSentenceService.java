package com.yunting.service;

import com.yunting.dto.original.OriginalSentenceListResponseDTO;

public interface OriginalSentenceService {
    OriginalSentenceListResponseDTO getOriginalSentenceList(Long taskId, Integer page, Integer pageSize);
    
    void deleteOriginalSentence(Long originalSentenceId);
}

