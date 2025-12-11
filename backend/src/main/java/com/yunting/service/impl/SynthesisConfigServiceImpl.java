package com.yunting.service.impl;

import com.yunting.constant.ReadingRuleApplicationType;
import com.yunting.constant.SynthesisStatus;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.PauseSettingMapper;
import com.yunting.mapper.PolyphonicSettingMapper;
import com.yunting.mapper.ProsodySettingMapper;
import com.yunting.mapper.ReadingRuleApplicationMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.PauseSetting;
import com.yunting.model.PolyphonicSetting;
import com.yunting.model.ProsodySetting;
import com.yunting.model.ReadingRuleApplication;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.service.SynthesisConfigService;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 合成配置服务实现
 */
@Service
public class SynthesisConfigServiceImpl implements SynthesisConfigService {

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
    private final PauseSettingMapper pauseSettingMapper;
    private final PolyphonicSettingMapper polyphonicSettingMapper;
    private final ProsodySettingMapper prosodySettingMapper;
    private final ReadingRuleApplicationMapper readingRuleApplicationMapper;

    public SynthesisConfigServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                     TaskMapper taskMapper,
                                     SynthesisSettingMapper synthesisSettingMapper,
                                     PauseSettingMapper pauseSettingMapper,
                                     PolyphonicSettingMapper polyphonicSettingMapper,
                                     ProsodySettingMapper prosodySettingMapper,
                                     ReadingRuleApplicationMapper readingRuleApplicationMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.pauseSettingMapper = pauseSettingMapper;
        this.polyphonicSettingMapper = polyphonicSettingMapper;
        this.prosodySettingMapper = prosodySettingMapper;
        this.readingRuleApplicationMapper = readingRuleApplicationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setConfig(SynthesisSetConfigRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        ValidationUtil.notNull(request.getTaskId(), "taskId不能为空");
        ValidationUtil.notNull(request.getOriginalSentenceId(), "originalSentenceId不能为空");
        ValidationUtil.notEmpty(request.getBreakingSentenceList(), "breakingSentenceList不能为空");

        // 验证任务是否存在
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }

        // 处理每个断句的配置
        for (SynthesisSetConfigRequest.BreakingSentenceConfig config : request.getBreakingSentenceList()) {
            ValidationUtil.notNull(config.getBreakingSentenceId(), "breakingSentenceId不能为空");
            
            Long breakingSentenceId = config.getBreakingSentenceId();
            boolean isNew = breakingSentenceId < 0;

            // 如果是新增，需要先创建断句记录
            if (isNew) {
                // 获取该任务下当前最大的sequence
                List<BreakingSentence> existingSentences = breakingSentenceMapper.selectByTaskId(request.getTaskId());
                int maxSequence = existingSentences.stream()
                        .mapToInt(BreakingSentence::getSequence)
                        .max()
                        .orElse(0);
                
                // 创建新的断句
                BreakingSentence newSentence = new BreakingSentence();
                newSentence.setTaskId(request.getTaskId());
                newSentence.setOriginalSentenceId(request.getOriginalSentenceId());
                newSentence.setContent(config.getContent());
                newSentence.setCharCount(calculateCharCount(config.getContent()));
                // 如果请求中提供了sequence，使用请求的值；否则使用最大值+1
                newSentence.setSequence(config.getSequence() != null ? config.getSequence() : maxSequence + 1);
                newSentence.setSynthesisStatus(SynthesisStatus.Status.PENDING);
                
                breakingSentenceMapper.insert(newSentence);
                breakingSentenceId = newSentence.getBreakingSentenceId();
            } else {
                // 验证断句是否存在
                BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
                if (sentence == null) {
                    throw new BusinessException(10404, "断句不存在: " + breakingSentenceId);
                }
                
                // 验证断句是否属于该任务
                if (!sentence.getTaskId().equals(request.getTaskId())) {
                    throw new BusinessException(10400, "断句不属于该任务");
                }
            }

            // 1. 更新content到breaking_sentences表
            if (StringUtils.hasText(config.getContent())) {
                int charCount = calculateCharCount(config.getContent());
                breakingSentenceMapper.updateContent(breakingSentenceId, config.getContent(), charCount);
            }

            // 1.1 更新sequence到breaking_sentences表（如果提供了sequence字段）
            if (config.getSequence() != null) {
                breakingSentenceMapper.updateSequence(breakingSentenceId, config.getSequence());
            }

            // 2. 更新volume、voiceId、speed到synthesis_settings表
            if (config.getVolume() != null || StringUtils.hasText(config.getVoiceId()) || config.getSpeed() != null) {
                SynthesisSetting setting = synthesisSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
                if (setting == null) {
                    setting = new SynthesisSetting();
                    setting.setBreakingSentenceId(breakingSentenceId);
                    // 设置默认值，避免数据库 NOT NULL 约束错误
                    setting.setSpeechRate(0);
                    setting.setVolume(0);
                    setting.setPitch(0);
                }
                
                if (config.getVolume() != null) {
                    setting.setVolume(config.getVolume());
                }
                if (StringUtils.hasText(config.getVoiceId())) {
                    setting.setVoiceId(config.getVoiceId());
                }
                if (config.getSpeed() != null) {
                    setting.setSpeechRate(config.getSpeed());
                }
                
                synthesisSettingMapper.upsert(setting);
            }

            // 3. 更新breakList和silenceList到pause_settings表（先删除旧的，再插入新的）
            pauseSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            List<PauseSetting> pauseSettings = new ArrayList<>();
            
            // 处理 breakList（type=1，停顿）
            if (!CollectionUtils.isEmpty(config.getBreakList())) {
                for (SynthesisSetConfigRequest.BreakConfig breakConfig : config.getBreakList()) {
                    PauseSetting pauseSetting = new PauseSetting();
                    pauseSetting.setBreakingSentenceId(breakingSentenceId);
                    pauseSetting.setPosition(breakConfig.getLocation());
                    pauseSetting.setDuration(breakConfig.getDuration());
                    pauseSetting.setType(1); // 1表示停顿
                    pauseSettings.add(pauseSetting);
                }
            }
            
            // 处理 silenceList（type=2，静音）
            if (!CollectionUtils.isEmpty(config.getSilenceList())) {
                for (SynthesisSetConfigRequest.SilenceConfig silenceConfig : config.getSilenceList()) {
                    PauseSetting pauseSetting = new PauseSetting();
                    pauseSetting.setBreakingSentenceId(breakingSentenceId);
                    pauseSetting.setPosition(silenceConfig.getLocation());
                    pauseSetting.setDuration(silenceConfig.getDuration());
                    pauseSetting.setType(2); // 2表示静音
                    pauseSettings.add(pauseSetting);
                }
            }
            
            if (!pauseSettings.isEmpty()) {
                pauseSettingMapper.insertBatch(pauseSettings);
            }

            // 4. 更新phonemeList到polyphonic_settings表（先删除旧的，再插入新的）
            polyphonicSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            if (!CollectionUtils.isEmpty(config.getPhonemeList())) {
                // 获取当前断句内容以提取字符
                BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
                String content = sentence != null ? sentence.getContent() : config.getContent();
                
                if (StringUtils.hasText(content)) {
                    List<PolyphonicSetting> polyphonicSettings = new ArrayList<>();
                    for (SynthesisSetConfigRequest.PhonemeConfig phonemeConfig : config.getPhonemeList()) {
                        if (phonemeConfig.getLocation() != null && phonemeConfig.getLocation() > 0 
                                && phonemeConfig.getLocation() <= content.length()
                                && StringUtils.hasText(phonemeConfig.getPh())) {
                            PolyphonicSetting polyphonicSetting = new PolyphonicSetting();
                            polyphonicSetting.setBreakingSentenceId(breakingSentenceId);
                            // 从content的location前一个位置提取字符
                            String word = content.substring(phonemeConfig.getLocation() - 1, phonemeConfig.getLocation());
                            polyphonicSetting.setWord(word);
                            polyphonicSetting.setPosition(phonemeConfig.getLocation());
                            polyphonicSetting.setPronunciation(phonemeConfig.getPh());
                            polyphonicSettings.add(polyphonicSetting);
                        }
                    }
                    if (!polyphonicSettings.isEmpty()) {
                        polyphonicSettingMapper.insertBatch(polyphonicSettings);
                    }
                }
            }

            // 5. 更新prosodyList到prosody_settings表（先删除旧的，再插入新的）
            prosodySettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            if (!CollectionUtils.isEmpty(config.getProsodyList())) {
                List<ProsodySetting> prosodySettings = new ArrayList<>();
                for (SynthesisSetConfigRequest.ProsodyConfig prosodyConfig : config.getProsodyList()) {
                    ProsodySetting prosodySetting = new ProsodySetting();
                    prosodySetting.setBreakingSentenceId(breakingSentenceId);
                    prosodySetting.setBeginPosition(prosodyConfig.getBegin());
                    prosodySetting.setEndPosition(prosodyConfig.getEnd());
                    prosodySetting.setRate(prosodyConfig.getRate());
                    prosodySettings.add(prosodySetting);
                }
                if (!prosodySettings.isEmpty()) {
                    prosodySettingMapper.insertBatch(prosodySettings);
                }
            }

            // 5.1 更新readRule到reading_rule_applications表（先删除旧的，再插入新的）
            readingRuleApplicationMapper.deleteByBreakingSentenceId(breakingSentenceId);
            if (!CollectionUtils.isEmpty(config.getReadRule())) {
                List<ReadingRuleApplication> readingRuleApplications = new ArrayList<>();
                for (SynthesisSetConfigRequest.ReadRuleConfig readRuleConfig : config.getReadRule()) {
                    // 处理所有规则，只要ruleId不为空就记录
                    if (readRuleConfig.getRuleId() != null) {
                        ReadingRuleApplication application = new ReadingRuleApplication();
                        application.setRuleId(readRuleConfig.getRuleId());
                        application.setFromId(breakingSentenceId);
                        application.setType(ReadingRuleApplicationType.Type.BREAKING_SENTENCE);
                        // 使用前端传入的isOpen值，如果为null则默认为false
                        application.setIsOpen(readRuleConfig.getIsOpen() != null ? readRuleConfig.getIsOpen() : false);
                        readingRuleApplications.add(application);
                    }
                }
                if (!readingRuleApplications.isEmpty()) {
                    readingRuleApplicationMapper.insertBatch(readingRuleApplications);
                }
            }

            // 6. 生成 SSML 并更新到 breaking_sentences 表
            // 获取最终的 content（可能已经更新）
            BreakingSentence finalSentence = breakingSentenceMapper.selectById(breakingSentenceId);
            String finalContent = finalSentence != null ? finalSentence.getContent() : config.getContent();
            
            if (StringUtils.hasText(finalContent)) {
                // 创建一个临时的 config 对象，使用最新的 content
                SynthesisSetConfigRequest.BreakingSentenceConfig ssmlConfig = new SynthesisSetConfigRequest.BreakingSentenceConfig();
                ssmlConfig.setContent(finalContent);
                ssmlConfig.setVoiceId(config.getVoiceId());
                ssmlConfig.setSpeed(config.getSpeed());
                ssmlConfig.setVolume(config.getVolume());
                ssmlConfig.setBreakList(config.getBreakList());
                ssmlConfig.setPhonemeList(config.getPhonemeList());
                ssmlConfig.setProsodyList(config.getProsodyList());
                ssmlConfig.setSilenceList(config.getSilenceList());
                
                String ssml = com.yunting.util.SsmlRenderer.renderFromConfig(ssmlConfig);
                if (StringUtils.hasText(ssml)) {
                    breakingSentenceMapper.updateSsml(breakingSentenceId, ssml);
                }
            }
        }
    }

    /**
     * 计算字符数
     */
    private int calculateCharCount(String content) {
        if (content == null) {
            return 0;
        }
        return content.length();
    }
}
