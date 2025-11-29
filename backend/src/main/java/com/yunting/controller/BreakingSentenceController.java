package com.yunting.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;
import com.yunting.dto.breaking.request.BreakingSentenceBatchSettingsRequest;
import com.yunting.dto.breaking.request.BreakingSentenceParamRequest;
import com.yunting.dto.breaking.request.*;
import com.yunting.exception.BusinessException;
import com.yunting.service.BreakingSentenceService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BreakingSentenceController {

    private final BreakingSentenceService breakingSentenceService;
    private final ObjectMapper objectMapper;

    public BreakingSentenceController(BreakingSentenceService breakingSentenceService, ObjectMapper objectMapper) {
        this.breakingSentenceService = breakingSentenceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/tasks/breaking-sentences")
    public ApiResponse<BreakingSentenceListResponseDTO> getBreakingSentenceList(
            @RequestParam("taskid") Long taskId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize) {
        BreakingSentenceListResponseDTO data = breakingSentenceService.getBreakingSentenceList(taskId, page, pageSize);
        return ResponseUtil.success(data);
    }

    @GetMapping("/breaking-sentences/info")
    public ApiResponse<BreakingSentenceDetailDTO> getBreakingSentenceDetail(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId) {
        BreakingSentenceDetailDTO detail = breakingSentenceService.getBreakingSentenceDetail(breakingSentenceId);
        return ResponseUtil.success(detail);
    }

    @DeleteMapping("/breaking-sentences")
    public ApiResponse<Map<String, Long>> deleteBreakingSentence(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId) {
        breakingSentenceService.deleteBreakingSentence(breakingSentenceId);
        return ResponseUtil.success(Map.of("breaking_sentence_id", breakingSentenceId));
    }

    @PostMapping(value = "/breaking-sentences/settings", params = "taskid")
    public ApiResponse<Map<String, Integer>> updateBreakingSentenceSettings(
            @RequestParam("taskid") Long taskId,
            @RequestBody BreakingSentenceBatchSettingsRequest request) {
        int updated = breakingSentenceService.updateBreakingSentenceParams(taskId,
                request != null ? request.getBreakingSentences() : null);
        return ResponseUtil.success(Map.of("updated_count", updated));
    }

    @PostMapping(value = "/breaking-sentences/settings", params = "breaking_sentence_id")
    public ApiResponse<Map<String, Long>> updateSingleBreakingSentenceSetting(
            @RequestParam("breaking_sentence_id") Long breakingSentenceId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "speech_rate", required = false) Integer speechRate,
            @RequestParam(value = "volume", required = false) Integer volume,
            @RequestParam(value = "pitch", required = false) Integer pitch,
            @RequestParam(value = "voice_id", required = false) String voiceId,
            @RequestParam(value = "pauses", required = false) String pausesJson,
            @RequestParam(value = "polyphonic", required = false) String polyphonicJson,
            @RequestParam(value = "say_as", required = false) String sayAsJson,
            @RequestParam(value = "sub", required = false) String subJson,
            @RequestParam(value = "word", required = false) String wordJson,
            @RequestParam(value = "emotion", required = false) String emotionJson,
            @RequestParam(value = "insert_action", required = false) String insertActionJson,
            @RequestParam(value = "reading_rule_ids", required = false) String readingRuleIdsStr) {
        
        // 构建请求对象
        BreakingSentenceParamRequest request = new BreakingSentenceParamRequest();
        request.setBreakingSentenceId(breakingSentenceId);
        request.setContent(content);
        request.setSpeechRate(speechRate);
        request.setVolume(volume);
        request.setPitch(pitch);
        request.setVoiceId(voiceId);
        
        // 解析 JSON 字符串参数
        try {
            if (StringUtils.hasText(pausesJson)) {
                List<PauseSettingRequest> pauses = objectMapper.readValue(pausesJson, 
                    new TypeReference<List<PauseSettingRequest>>() {});
                request.setPauses(pauses);
            }
            
            if (StringUtils.hasText(polyphonicJson)) {
                List<PolyphonicSettingRequest> polyphonic = objectMapper.readValue(polyphonicJson, 
                    new TypeReference<List<PolyphonicSettingRequest>>() {});
                request.setPolyphonic(polyphonic);
            }
            
            if (StringUtils.hasText(sayAsJson)) {
                List<SayAsSettingRequest> sayAs = objectMapper.readValue(sayAsJson, 
                    new TypeReference<List<SayAsSettingRequest>>() {});
                request.setSayAs(sayAs);
            }
            
            if (StringUtils.hasText(subJson)) {
                List<SubSettingRequest> sub = objectMapper.readValue(subJson, 
                    new TypeReference<List<SubSettingRequest>>() {});
                request.setSub(sub);
            }
            
            if (StringUtils.hasText(wordJson)) {
                List<WordSettingRequest> word = objectMapper.readValue(wordJson, 
                    new TypeReference<List<WordSettingRequest>>() {});
                request.setWord(word);
            }
            
            if (StringUtils.hasText(emotionJson)) {
                List<EmotionSettingRequest> emotion = objectMapper.readValue(emotionJson, 
                    new TypeReference<List<EmotionSettingRequest>>() {});
                request.setEmotion(emotion);
            }
            
            if (StringUtils.hasText(insertActionJson)) {
                List<InsertActionSettingRequest> insertAction = objectMapper.readValue(insertActionJson, 
                    new TypeReference<List<InsertActionSettingRequest>>() {});
                request.setInsertAction(insertAction);
            }
            
            if (StringUtils.hasText(readingRuleIdsStr)) {
                // 支持逗号分隔的 ID 列表，如 "1,2,3"
                List<Long> readingRuleIds = new ArrayList<>();
                String[] ids = readingRuleIdsStr.split(",");
                for (String id : ids) {
                    if (StringUtils.hasText(id.trim())) {
                        readingRuleIds.add(Long.parseLong(id.trim()));
                    }
                }
                request.setReadingRuleIds(readingRuleIds);
            }
        } catch (Exception e) {
            throw new BusinessException(10400, "参数解析失败: " + e.getMessage());
        }
        
        breakingSentenceService.updateBreakingSentenceParam(breakingSentenceId, request);
        return ResponseUtil.success(Map.of("breaking_sentence_id", breakingSentenceId));
    }
}


