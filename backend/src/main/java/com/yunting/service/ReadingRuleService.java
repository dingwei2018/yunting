package com.yunting.service;

import com.yunting.dto.reading.ReadingRuleApplyResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleDTO;
import com.yunting.dto.reading.ReadingRuleListResponseDTO;

public interface ReadingRuleService {

    ReadingRuleDTO createReadingRule(ReadingRuleCreateRequest request);

    ReadingRuleListResponseDTO getReadingRules(Long taskId, Integer scope);

    ReadingRuleApplyResponseDTO applyReadingRule(Long ruleId, Long taskId);
}


