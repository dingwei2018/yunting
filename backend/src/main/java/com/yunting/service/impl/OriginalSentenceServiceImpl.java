package com.yunting.service.impl;

import com.yunting.dto.original.*;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.*;
import com.yunting.model.*;
import com.yunting.service.OriginalSentenceService;
import com.yunting.util.SynthesisStatusUtil;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OriginalSentenceServiceImpl implements OriginalSentenceService {

    private final OriginalSentenceMapper originalSentenceMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
    private final PauseSettingMapper pauseSettingMapper;
    private final PolyphonicSettingMapper polyphonicSettingMapper;
    private final ProsodySettingMapper prosodySettingMapper;
    private final ReadingRuleApplicationMapper readingRuleApplicationMapper;
    private final TaskMapper taskMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OriginalSentenceServiceImpl(OriginalSentenceMapper originalSentenceMapper,
                                       BreakingSentenceMapper breakingSentenceMapper,
                                       SynthesisSettingMapper synthesisSettingMapper,
                                       PauseSettingMapper pauseSettingMapper,
                                       PolyphonicSettingMapper polyphonicSettingMapper,
                                       ProsodySettingMapper prosodySettingMapper,
                                       ReadingRuleApplicationMapper readingRuleApplicationMapper,
                                       TaskMapper taskMapper) {
        this.originalSentenceMapper = originalSentenceMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.pauseSettingMapper = pauseSettingMapper;
        this.polyphonicSettingMapper = polyphonicSettingMapper;
        this.prosodySettingMapper = prosodySettingMapper;
        this.readingRuleApplicationMapper = readingRuleApplicationMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public OriginalSentenceListResponseDTO getOriginalSentenceList(Long taskId, Integer page, Integer pageSize) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }

        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (currentPage - 1) * size;

        long total = originalSentenceMapper.countByTaskId(taskId);
        List<OriginalSentenceListItemDTO> list;
        if (total == 0) {
            list = Collections.emptyList();
        } else {
            List<OriginalSentence> originalSentences = originalSentenceMapper.selectPageByTaskId(taskId, offset, size);
            list = mapToOriginalSentenceListDTO(originalSentences);
        }

        OriginalSentenceListResponseDTO response = new OriginalSentenceListResponseDTO();
        response.setList(list);
        response.setTotal(total);
        response.setPage(currentPage);
        response.setPageSize(size);
        return response;
    }

    private List<OriginalSentenceListItemDTO> mapToOriginalSentenceListDTO(List<OriginalSentence> originalSentences) {
        if (originalSentences.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有拆句ID
        List<Long> originalSentenceIds = originalSentences.stream()
                .map(OriginalSentence::getOriginalSentenceId)
                .collect(Collectors.toList());

        // 查询所有拆句下的断句
        List<BreakingSentence> allBreakingSentences = breakingSentenceMapper.selectByTaskId(
                originalSentences.get(0).getTaskId());
        Map<Long, List<BreakingSentence>> breakingSentencesByOriginal = allBreakingSentences.stream()
                .filter(bs -> bs.getOriginalSentenceId() != null && originalSentenceIds.contains(bs.getOriginalSentenceId()))
                .collect(Collectors.groupingBy(BreakingSentence::getOriginalSentenceId));

        // 获取所有断句ID
        List<Long> breakingSentenceIds = allBreakingSentences.stream()
                .map(BreakingSentence::getBreakingSentenceId)
                .collect(Collectors.toList());

        // 批量查询设置
        Map<Long, SynthesisSetting> synthesisSettingMap = fetchSynthesisSettings(breakingSentenceIds);
        Map<Long, List<PauseSetting>> pauseSettingMap = fetchPauseSettings(breakingSentenceIds);
        Map<Long, List<PolyphonicSetting>> polyphonicSettingMap = fetchPolyphonicSettings(breakingSentenceIds);
        Map<Long, List<ProsodySetting>> prosodySettingMap = fetchProsodySettings(breakingSentenceIds);

        return originalSentences.stream()
                .map(original -> {
                    OriginalSentenceListItemDTO dto = new OriginalSentenceListItemDTO();
                    dto.setOriginalSentenceId(original.getOriginalSentenceId());
                    dto.setSequence(original.getSequence() != null ? original.getSequence() : 0);
                    dto.setContent(original.getContent() != null ? original.getContent() : "");

                    // 获取该拆句下的所有断句
                    List<BreakingSentence> breakingSentences = breakingSentencesByOriginal.getOrDefault(
                            original.getOriginalSentenceId(), Collections.emptyList());

                    // 聚合合成状态
                    Integer synthesisStatus = aggregateSynthesisStatus(breakingSentences);
                    dto.setSynthesisStatus(synthesisStatus != null ? synthesisStatus : 0);

                    // 构建断句列表
                    List<BreakingSentenceWithSettingDTO> breakingSentenceDTOs = breakingSentences.stream()
                            .sorted(Comparator.comparing(BreakingSentence::getSequence))
                            .map(bs -> toBreakingSentenceWithSettingDTO(
                                    bs,
                                    synthesisSettingMap.get(bs.getBreakingSentenceId()),
                                    pauseSettingMap.getOrDefault(bs.getBreakingSentenceId(), Collections.emptyList()),
                                    polyphonicSettingMap.getOrDefault(bs.getBreakingSentenceId(), Collections.emptyList()),
                                    prosodySettingMap.getOrDefault(bs.getBreakingSentenceId(), Collections.emptyList())
                            ))
                            .collect(Collectors.toList());

                    dto.setBreakingSentenceList(breakingSentenceDTOs);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Integer aggregateSynthesisStatus(List<BreakingSentence> breakingSentences) {
        return SynthesisStatusUtil.aggregateSynthesisStatus(breakingSentences);
    }

    private BreakingSentenceWithSettingDTO toBreakingSentenceWithSettingDTO(
            BreakingSentence breakingSentence,
            SynthesisSetting synthesisSetting,
            List<PauseSetting> pauseSettings,
            List<PolyphonicSetting> polyphonicSettings,
            List<ProsodySetting> prosodySettings) {
        BreakingSentenceWithSettingDTO dto = new BreakingSentenceWithSettingDTO();
        dto.setBreakingSentenceId(breakingSentence.getBreakingSentenceId());
        dto.setTaskId(breakingSentence.getTaskId());
        dto.setOriginalSentenceId(breakingSentence.getOriginalSentenceId());
        dto.setContent(breakingSentence.getContent() != null ? breakingSentence.getContent() : "");
        dto.setCharCount(breakingSentence.getCharCount() != null ? breakingSentence.getCharCount() : 0);
        dto.setSequence(breakingSentence.getSequence() != null ? breakingSentence.getSequence() : 0);
        dto.setSynthesisStatus(breakingSentence.getSynthesisStatus() != null ? breakingSentence.getSynthesisStatus() : 0);
        dto.setAudioUrl(breakingSentence.getAudioUrl() != null ? breakingSentence.getAudioUrl() : "");
        dto.setAudioDuration(breakingSentence.getAudioDuration() != null ? breakingSentence.getAudioDuration() : 0);
        dto.setSsml(breakingSentence.getSsml() != null ? breakingSentence.getSsml() : "");
        dto.setJobId(breakingSentence.getJobId() != null ? breakingSentence.getJobId() : "");
        dto.setCreatedAt(breakingSentence.getCreatedAt() != null ? 
                breakingSentence.getCreatedAt().format(DATE_TIME_FORMATTER) : "");
        dto.setUpdatedAt(breakingSentence.getUpdatedAt() != null ? 
                breakingSentence.getUpdatedAt().format(DATE_TIME_FORMATTER) : "");

        // 构建 setting
        BreakingSentenceSettingDTO settingDTO = buildSettingDTO(
                breakingSentence,
                synthesisSetting,
                pauseSettings,
                polyphonicSettings,
                prosodySettings);
        dto.setSetting(settingDTO);

        return dto;
    }

    private BreakingSentenceSettingDTO buildSettingDTO(
            BreakingSentence breakingSentence,
            SynthesisSetting synthesisSetting,
            List<PauseSetting> pauseSettings,
            List<PolyphonicSetting> polyphonicSettings,
            List<ProsodySetting> prosodySettings) {
        BreakingSentenceSettingDTO settingDTO = new BreakingSentenceSettingDTO();
        settingDTO.setContent(breakingSentence.getContent() != null ? breakingSentence.getContent() : "");
        settingDTO.setVolume(synthesisSetting != null && synthesisSetting.getVolume() != null ? 
                synthesisSetting.getVolume() : 0);
        settingDTO.setSpeed(synthesisSetting != null && synthesisSetting.getSpeechRate() != null ? 
                synthesisSetting.getSpeechRate() : 0);
        settingDTO.setVoiceId(synthesisSetting != null && synthesisSetting.getVoiceId() != null ? 
                synthesisSetting.getVoiceId() : "");

        // 构建 breakList (type=1) 和 silentList (type=2)
        List<BreakConfigDTO> breakList = new ArrayList<>();
        List<SilenceConfigDTO> silentList = new ArrayList<>();
        for (PauseSetting pause : pauseSettings) {
            if (pause.getType() != null) {
                if (pause.getType() == 1) {
                    // 停顿
                    BreakConfigDTO breakConfig = new BreakConfigDTO();
                    breakConfig.setLocation(pause.getPosition() != null ? String.valueOf(pause.getPosition()) : "");
                    breakConfig.setDuration(pause.getDuration() != null ? String.valueOf(pause.getDuration()) : "");
                    breakList.add(breakConfig);
                } else if (pause.getType() == 2) {
                    // 静音
                    SilenceConfigDTO silenceConfig = new SilenceConfigDTO();
                    silenceConfig.setLocation(pause.getPosition() != null ? pause.getPosition() : 0);
                    silenceConfig.setDuration(pause.getDuration() != null ? pause.getDuration() : 0);
                    silentList.add(silenceConfig);
                }
            }
        }
        settingDTO.setBreakList(breakList);
        settingDTO.setSilentList(silentList);

        // 构建 phonemeList
        List<PhonemeConfigDTO> phonemeList = polyphonicSettings.stream()
                .map(poly -> {
                    PhonemeConfigDTO phoneme = new PhonemeConfigDTO();
                    phoneme.setPh(poly.getPronunciation() != null ? poly.getPronunciation() : "");
                    phoneme.setLocation(poly.getPosition() != null ? poly.getPosition() : 0);
                    return phoneme;
                })
                .collect(Collectors.toList());
        settingDTO.setPhonemeList(phonemeList);

        // 构建 prosodyList
        List<ProsodyConfigDTO> prosodyList = prosodySettings.stream()
                .map(prosody -> {
                    ProsodyConfigDTO prosodyConfig = new ProsodyConfigDTO();
                    prosodyConfig.setRate(prosody.getRate() != null ? String.valueOf(prosody.getRate()) : "");
                    prosodyConfig.setBegin(prosody.getBeginPosition() != null ? prosody.getBeginPosition() : 0);
                    prosodyConfig.setEnd(prosody.getEndPosition() != null ? prosody.getEndPosition() : 0);
                    return prosodyConfig;
                })
                .collect(Collectors.toList());
        settingDTO.setProsodyList(prosodyList);

        return settingDTO;
    }

    private Map<Long, SynthesisSetting> fetchSynthesisSettings(List<Long> breakingSentenceIds) {
        if (breakingSentenceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SynthesisSetting> settings = synthesisSettingMapper.selectByBreakingSentenceIds(breakingSentenceIds);
        return settings.stream()
                .collect(Collectors.toMap(SynthesisSetting::getBreakingSentenceId, s -> s));
    }

    private Map<Long, List<PauseSetting>> fetchPauseSettings(List<Long> breakingSentenceIds) {
        if (breakingSentenceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PauseSetting> settings = pauseSettingMapper.selectByBreakingSentenceIds(breakingSentenceIds);
        return settings.stream()
                .collect(Collectors.groupingBy(PauseSetting::getBreakingSentenceId));
    }

    private Map<Long, List<PolyphonicSetting>> fetchPolyphonicSettings(List<Long> breakingSentenceIds) {
        if (breakingSentenceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PolyphonicSetting> settings = polyphonicSettingMapper.selectByBreakingSentenceIds(breakingSentenceIds);
        return settings.stream()
                .collect(Collectors.groupingBy(PolyphonicSetting::getBreakingSentenceId));
    }

    private Map<Long, List<ProsodySetting>> fetchProsodySettings(List<Long> breakingSentenceIds) {
        if (breakingSentenceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProsodySetting> settings = prosodySettingMapper.selectByBreakingSentenceIds(breakingSentenceIds);
        return settings.stream()
                .collect(Collectors.groupingBy(ProsodySetting::getBreakingSentenceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOriginalSentence(Long originalSentenceId) {
        // 1. 参数验证
        ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
        
        // 2. 验证拆句是否存在
        OriginalSentence originalSentence = originalSentenceMapper.selectById(originalSentenceId);
        if (originalSentence == null) {
            throw new BusinessException(10404, "拆句不存在");
        }
        
        // 3. 查询该拆句下的所有断句
        List<BreakingSentence> breakingSentences = breakingSentenceMapper.selectByOriginalSentenceId(originalSentenceId);
        
        // 4. 删除每个断句的所有配置
        for (BreakingSentence breakingSentence : breakingSentences) {
            Long breakingSentenceId = breakingSentence.getBreakingSentenceId();
            
            // 删除合成设置
            synthesisSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            
            // 删除停顿设置
            pauseSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            
            // 删除多音字设置
            polyphonicSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            
            // 删除韵律设置
            prosodySettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            
            // 删除阅读规范应用
            readingRuleApplicationMapper.deleteByBreakingSentenceId(breakingSentenceId);
        }
        
        // 5. 删除所有断句
        for (BreakingSentence breakingSentence : breakingSentences) {
            breakingSentenceMapper.deleteById(breakingSentence.getBreakingSentenceId());
        }
        
        // 6. 删除拆句本身
        int deleted = originalSentenceMapper.deleteById(originalSentenceId);
        if (deleted == 0) {
            throw new BusinessException(10500, "删除拆句失败");
        }
    }
}

