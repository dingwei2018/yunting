package com.yunting.service;

import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;
import com.yunting.dto.breaking.request.BreakingSentenceParamRequest;

import java.util.List;

public interface BreakingSentenceService {

    BreakingSentenceListResponseDTO getBreakingSentenceList(Long taskId, Integer page, Integer pageSize);

    BreakingSentenceDetailDTO getBreakingSentenceDetail(Long breakingSentenceId);

    void deleteBreakingSentence(Long breakingSentenceId);

    int updateBreakingSentenceParams(Long taskId, List<BreakingSentenceParamRequest> requests);

    void updateBreakingSentenceParam(Long breakingSentenceId, BreakingSentenceParamRequest request);
}


