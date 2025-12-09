package com.yunting.service.impl;

import com.yunting.dto.breaking.BreakingSentenceDetailDTO;
import com.yunting.dto.breaking.BreakingSentenceListItemDTO;
import com.yunting.dto.breaking.BreakingSentenceListResponseDTO;
import com.yunting.dto.breaking.BreakingSentenceSettingsDTO;
import com.yunting.dto.breaking.PauseDTO;
import com.yunting.dto.breaking.PolyphonicSettingDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.PauseSettingMapper;
import com.yunting.mapper.PolyphonicSettingMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.mapper.ReadingRuleMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.PauseSetting;
import com.yunting.model.PolyphonicSetting;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.service.BreakingSentenceService;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

    public BreakingSentenceServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                       TaskMapper taskMapper,
                                       SynthesisSettingMapper synthesisSettingMapper,
                                       PauseSettingMapper pauseSettingMapper,
                                       PolyphonicSettingMapper polyphonicSettingMapper,
                                       ReadingRuleMapper readingRuleMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.pauseSettingMapper = pauseSettingMapper;
        this.polyphonicSettingMapper = polyphonicSettingMapper;
        this.readingRuleMapper = readingRuleMapper;
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
        dto.setCharacter(polyphonicSetting.getWord());
        dto.setPosition(polyphonicSetting.getPosition());
        dto.setPronunciation(polyphonicSetting.getPronunciation());
        return dto;
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


