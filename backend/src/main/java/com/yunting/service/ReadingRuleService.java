package com.yunting.service;

import com.yunting.dto.reading.MatchingFieldListResponseDTO;
import com.yunting.dto.reading.ReadingRuleApplyResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleCreateResponseDTO;
import com.yunting.dto.reading.ReadingRuleDTO;
import com.yunting.dto.reading.ReadingRuleListPageResponseDTO;
import com.yunting.dto.reading.ReadingRuleListResponseDTO;
import com.yunting.dto.reading.ReadingRuleSetGlobalSettingRequest;

public interface ReadingRuleService {

    ReadingRuleCreateResponseDTO createReadingRule(ReadingRuleCreateRequest request);

    ReadingRuleListResponseDTO getReadingRules();

    ReadingRuleApplyResponseDTO applyReadingRule(Long ruleId, Long taskId);

    String setGlobalSetting(ReadingRuleSetGlobalSettingRequest request);

    ReadingRuleListPageResponseDTO getReadingRuleList(Long taskId, Integer ruleType, Integer page, Integer pageSize);

    MatchingFieldListResponseDTO getMatchingFieldListFromText(String text);
}


