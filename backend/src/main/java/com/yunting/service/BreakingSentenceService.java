package com.yunting.service;

import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;

public interface BreakingSentenceService {

    BreakingSentenceListResponseDTO getBreakingSentenceList(Long taskId, Integer page, Integer pageSize);

    BreakingSentenceDetailDTO getBreakingSentenceDetail(Long breakingSentenceId);

    void deleteBreakingSentence(Long breakingSentenceId);
}


