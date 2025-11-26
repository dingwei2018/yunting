package com.yunting.service.impl;

import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListItemDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;
import com.yunting.dto.breaking.BreakingSentenceSettingsDTO;
import com.yunting.dto.breaking.PauseDTO;
import com.yunting.dto.breaking.PolyphonicSettingDTO;
import com.yunting.dto.breaking.request.BreakingSentenceParamRequest;
import com.yunting.dto.breaking.request.PauseSettingRequest;
import com.yunting.dto.breaking.request.PolyphonicSettingRequest;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.PauseSettingMapper;
import com.yunting.mapper.PolyphonicSettingMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.mapper.ReadingRuleApplicationMapper;
import com.yunting.mapper.ReadingRuleMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.PauseSetting;
import com.yunting.model.PolyphonicSetting;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.model.ReadingRuleApplication;
import com.yunting.service.BreakingSentenceService;
import com.yunting.util.SsmlRenderer;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BreakingSentenceServiceImpl implements BreakingSentenceService {

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
    private final PauseSettingMapper pauseSettingMapper;
    private final PolyphonicSettingMapper polyphonicSettingMapper;
    private final ReadingRuleMapper readingRuleMapper;
    private final ReadingRuleApplicationMapper readingRuleApplicationMapper;

    public BreakingSentenceServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                       TaskMapper taskMapper,
                                       SynthesisSettingMapper synthesisSettingMapper,
                                       PauseSettingMapper pauseSettingMapper,
                                       PolyphonicSettingMapper polyphonicSettingMapper,
                                       ReadingRuleMapper readingRuleMapper,
                                       ReadingRuleApplicationMapper readingRuleApplicationMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.pauseSettingMapper = pauseSettingMapper;
        this.polyphonicSettingMapper = polyphonicSettingMapper;
        this.readingRuleMapper = readingRuleMapper;
        this.readingRuleApplicationMapper = readingRuleApplicationMapper;
    }

    @Override
    public BreakingSentenceListResponseDTO getBreakingSentenceList(Long taskId, Integer page, Integer pageSize) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (currentPage - 1) * size;

        long total = breakingSentenceMapper.countByTaskId(taskId);
        List<BreakingSentenceListItemDTO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            List<BreakingSentence> sentences = breakingSentenceMapper.selectPageByTaskId(taskId, offset, size);
            list = mapToListDTO(sentences);
        }

        BreakingSentenceListResponseDTO response = new BreakingSentenceListResponseDTO();
        response.setList(list);
        response.setTotal(total);
        response.setPage(currentPage);
        response.setPageSize(size);
        return response;
    }

    @Override
    public BreakingSentenceDetailDTO getBreakingSentenceDetail(Long breakingSentenceId) {
        ValidationUtil.notNull(breakingSentenceId, "breaking_sentence_id不能为空");
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            throw new BusinessException(10404, "断句不存在");
        }
        BreakingSentenceDetailDTO detail = new BreakingSentenceDetailDTO();
        populateBaseFields(detail, sentence);

        SynthesisSetting setting = synthesisSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
        detail.setSettings(toSettingsDTO(setting));

        List<PauseSetting> pauses = pauseSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
        detail.setPauses(pauses.stream().map(this::toPauseDTO).collect(Collectors.toList()));

        List<PolyphonicSetting> polyphonicSettings = polyphonicSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
        detail.setPolyphonicSettings(polyphonicSettings.stream().map(this::toPolyphonicDTO).collect(Collectors.toList()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBreakingSentence(Long breakingSentenceId) {
        ValidationUtil.notNull(breakingSentenceId, "breaking_sentence_id不能为空");
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            throw new BusinessException(10404, "断句不存在");
        }
        int deleted = breakingSentenceMapper.deleteById(breakingSentenceId);
        if (deleted == 0) {
            throw new BusinessException(10500, "删除断句失败");
        }
        breakingSentenceMapper.decrementSequenceAfter(sentence.getTaskId(), sentence.getSequence());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBreakingSentenceParams(Long taskId, List<BreakingSentenceParamRequest> requests) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        if (CollectionUtils.isEmpty(requests)) {
            return 0;
        }
        List<Long> ids = requests.stream()
                .map(BreakingSentenceParamRequest::getBreakingSentenceId)
                .collect(Collectors.toList());
        ValidationUtil.notEmpty(ids, "breaking_sentence_id不能为空");

        List<BreakingSentence> sentences = breakingSentenceMapper.selectByIds(ids);
        if (sentences.size() != ids.size()) {
            throw new BusinessException(10404, "部分断句不存在");
        }
        boolean invalid = sentences.stream().anyMatch(s -> !taskId.equals(s.getTaskId()));
        if (invalid) {
            throw new BusinessException(10400, "断句不属于当前任务");
        }

        Map<Long, BreakingSentence> sentenceMap = sentences.stream()
                .collect(Collectors.toMap(BreakingSentence::getBreakingSentenceId, Function.identity()));

        for (BreakingSentenceParamRequest request : requests) {
            applyParamUpdate(sentenceMap.get(request.getBreakingSentenceId()), request);
        }
        return requests.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBreakingSentenceParam(Long breakingSentenceId, BreakingSentenceParamRequest request) {
        ValidationUtil.notNull(breakingSentenceId, "breaking_sentence_id不能为空");
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            throw new BusinessException(10404, "断句不存在");
        }
        request.setBreakingSentenceId(breakingSentenceId);
        applyParamUpdate(sentence, request);
    }

    private List<BreakingSentenceListItemDTO> mapToListDTO(List<BreakingSentence> sentences) {
        if (sentences.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> breakingIds = sentences.stream()
                .map(BreakingSentence::getBreakingSentenceId)
                .collect(Collectors.toList());

        Map<Long, SynthesisSetting> settingMap = fetchSettings(breakingIds);

        return sentences.stream()
                .map(sentence -> {
                    BreakingSentenceListItemDTO dto = new BreakingSentenceListItemDTO();
                    populateBaseFields(dto, sentence);
                    dto.setSettings(toSettingsDTO(settingMap.get(sentence.getBreakingSentenceId())));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, SynthesisSetting> fetchSettings(List<Long> breakingIds) {
        if (breakingIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SynthesisSetting> settings = synthesisSettingMapper.selectByBreakingSentenceIds(breakingIds);
        return settings.stream()
                .collect(Collectors.toMap(SynthesisSetting::getBreakingSentenceId, Function.identity()));
    }

    private void populateBaseFields(BreakingSentenceListItemDTO dto, BreakingSentence sentence) {
        dto.setBreakingSentenceId(sentence.getBreakingSentenceId());
        dto.setTaskId(sentence.getTaskId());
        dto.setOriginalSentenceId(sentence.getOriginalSentenceId());
        dto.setSequence(sentence.getSequence());
        dto.setContent(sentence.getContent());
        dto.setSynthesisStatus(sentence.getSynthesisStatus());
        dto.setAudioUrl(sentence.getAudioUrl());
        dto.setAudioDuration(sentence.getAudioDuration());
        dto.setSsml(sentence.getSsml());
    }

    private BreakingSentenceSettingsDTO toSettingsDTO(SynthesisSetting setting) {
        BreakingSentenceSettingsDTO dto = new BreakingSentenceSettingsDTO();
        if (setting != null) {
            dto.setVoiceId(setting.getVoiceId());
            dto.setVoiceName(setting.getVoiceName());
            dto.setSpeechRate(setting.getSpeechRate());
            dto.setVolume(setting.getVolume());
            dto.setPitch(setting.getPitch());
        } else {
            dto.setVoiceId(null);
            dto.setVoiceName(null);
            dto.setSpeechRate(0);
            dto.setVolume(0);
            dto.setPitch(0);
        }
        return dto;
    }

    private PauseDTO toPauseDTO(PauseSetting pauseSetting) {
        PauseDTO dto = new PauseDTO();
        dto.setId(pauseSetting.getPauseId());
        dto.setPosition(pauseSetting.getPosition());
        dto.setDuration(pauseSetting.getDuration());
        dto.setType(pauseSetting.getType());
        return dto;
    }

    private PolyphonicSettingDTO toPolyphonicDTO(PolyphonicSetting polyphonicSetting) {
        PolyphonicSettingDTO dto = new PolyphonicSettingDTO();
        dto.setId(polyphonicSetting.getPolyphonicId());
        dto.setCharacter(polyphonicSetting.getCharacter());
        dto.setPosition(polyphonicSetting.getPosition());
        dto.setPronunciation(polyphonicSetting.getPronunciation());
        return dto;
    }

    private void applyParamUpdate(BreakingSentence sentence, BreakingSentenceParamRequest request) {
        if (request == null) {
            return;
        }
        boolean contentUpdated = false;
        if (StringUtils.hasText(request.getContent())) {
            breakingSentenceMapper.updateContent(sentence.getBreakingSentenceId(),
                    request.getContent(),
                    request.getContent().length());
            sentence.setContent(request.getContent());
            contentUpdated = true;
        }

        boolean hasSettingUpdate = request.getVoiceId() != null ||
                request.getSpeechRate() != null ||
                request.getVolume() != null ||
                request.getPitch() != null;

        if (hasSettingUpdate) {
            SynthesisSetting existing = synthesisSettingMapper.selectByBreakingSentenceId(sentence.getBreakingSentenceId());
            SynthesisSetting setting = new SynthesisSetting();
            setting.setBreakingSentenceId(sentence.getBreakingSentenceId());
            setting.setVoiceId(request.getVoiceId() != null ? request.getVoiceId() :
                    existing != null ? existing.getVoiceId() : null);
            setting.setVoiceName(existing != null ? existing.getVoiceName() : null);
            setting.setSpeechRate(request.getSpeechRate() != null ? request.getSpeechRate() :
                    existing != null ? existing.getSpeechRate() : 0);
            setting.setVolume(request.getVolume() != null ? request.getVolume() :
                    existing != null ? existing.getVolume() : 0);
            setting.setPitch(request.getPitch() != null ? request.getPitch() :
                    existing != null ? existing.getPitch() : 0);
            synthesisSettingMapper.upsert(setting);
            contentUpdated = true;
        }

        if (request.getPauses() != null) {
            pauseSettingMapper.deleteByBreakingSentenceId(sentence.getBreakingSentenceId());
            if (!request.getPauses().isEmpty()) {
                List<PauseSetting> pauses = request.getPauses().stream().map(p -> {
                    PauseSetting model = new PauseSetting();
                    model.setBreakingSentenceId(sentence.getBreakingSentenceId());
                    model.setPosition(p.getPosition());
                    model.setDuration(p.getDuration());
                    model.setType(p.getType());
                    return model;
                }).collect(Collectors.toList());
                pauseSettingMapper.insertBatch(pauses);
            }
        }

        if (request.getPolyphonic() != null) {
            polyphonicSettingMapper.deleteByBreakingSentenceId(sentence.getBreakingSentenceId());
            if (!request.getPolyphonic().isEmpty()) {
                List<PolyphonicSetting> list = request.getPolyphonic().stream().map(p -> {
                    PolyphonicSetting model = new PolyphonicSetting();
                    model.setBreakingSentenceId(sentence.getBreakingSentenceId());
                    model.setCharacter(p.getCharacter());
                    model.setPosition(p.getPosition());
                    model.setPronunciation(p.getPronunciation());
                    return model;
                }).collect(Collectors.toList());
                polyphonicSettingMapper.insertBatch(list);
            }
        }

        if (request.getReadingRuleIds() != null) {
            validateReadingRules(request.getReadingRuleIds());
            readingRuleApplicationMapper.deleteByBreakingSentenceId(sentence.getBreakingSentenceId());
            if (!request.getReadingRuleIds().isEmpty()) {
                List<ReadingRuleApplication> applications = request.getReadingRuleIds().stream()
                        .map(ruleId -> {
                            ReadingRuleApplication model = new ReadingRuleApplication();
                            model.setRuleId(ruleId);
                            model.setBreakingSentenceId(sentence.getBreakingSentenceId());
                            return model;
                        }).collect(Collectors.toList());
                readingRuleApplicationMapper.insertBatch(applications);
            }
        }

        if (contentUpdated || request.getPauses() != null || request.getPolyphonic() != null) {
            SynthesisSetting setting = synthesisSettingMapper.selectByBreakingSentenceId(sentence.getBreakingSentenceId());
            String ssml = SsmlRenderer.render(sentence.getContent(), setting);
            breakingSentenceMapper.updateSsml(sentence.getBreakingSentenceId(), ssml);
        }
    }

    private void validateReadingRules(List<Long> readingRuleIds) {
        if (CollectionUtils.isEmpty(readingRuleIds)) {
            return;
        }
        List<Long> existing = readingRuleMapper.selectExistingIds(readingRuleIds);
        if (existing.size() != readingRuleIds.size()) {
            throw new BusinessException(10404, "存在无效的阅读规范ID");
        }
    }
}


