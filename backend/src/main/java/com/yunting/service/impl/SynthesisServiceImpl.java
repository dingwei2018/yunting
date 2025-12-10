package com.yunting.service.impl;

import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;
import com.yunting.dto.synthesis.TtsSynthesisRequest;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.service.SynthesisService;
import com.yunting.service.SynthesisConfigService;
import com.yunting.service.SynthesisStatusService;
import com.yunting.service.TtsCallbackHandlerService;
import com.yunting.service.RocketMQTtsSynthesisService;
import com.yunting.util.ValidationUtil;
import com.yunting.constant.SynthesisStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SynthesisServiceImpl implements SynthesisService {

    private static final Logger logger = LoggerFactory.getLogger(SynthesisServiceImpl.class);
    
    // 默认 voiceId
    private static final String DEFAULT_VOICE_ID = "c41f12c125f24c834ed3ae7c1fdae456";

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
    private final RocketMQTtsSynthesisService rocketMQTtsSynthesisService;
    private final SynthesisConfigService synthesisConfigService;
    private final SynthesisStatusService synthesisStatusService;
    private final TtsCallbackHandlerService ttsCallbackHandlerService;

    public SynthesisServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                TaskMapper taskMapper,
                                SynthesisSettingMapper synthesisSettingMapper,
                                RocketMQTtsSynthesisService rocketMQTtsSynthesisService,
                                SynthesisConfigService synthesisConfigService,
                                SynthesisStatusService synthesisStatusService,
                                TtsCallbackHandlerService ttsCallbackHandlerService) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.rocketMQTtsSynthesisService = rocketMQTtsSynthesisService;
        this.synthesisConfigService = synthesisConfigService;
        this.synthesisStatusService = synthesisStatusService;
        this.ttsCallbackHandlerService = ttsCallbackHandlerService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String synthesize(Long breakingSentenceId) {
        try {
            // 1. 参数验证：确保断句ID不为空
            ValidationUtil.notNull(breakingSentenceId, "breakingSentenceId不能为空");
            
            // 2. 查询断句信息，验证断句是否存在
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence == null) {
                logger.warn("断句不存在，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
                return SynthesisStatus.Message.FAILED;
            }

            // 3. 验证 SSML 是否存在（SSML 现在有默认值，但还是要验证）
            if (!StringUtils.hasText(sentence.getSsml())) {
                logger.warn("断句的SSML为空，无法进行合成，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
                return SynthesisStatus.Message.FAILED;
            }

            // 4. 从 synthesis_settings 表中读取合成参数（虽然使用SSML，但保留这些参数以备后用）
            SynthesisSetting setting = synthesisSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
            String voiceId = null;
            Integer speechRate = null;
            Integer volume = null;
            Integer pitch = null;
            
            if (setting != null) {
                voiceId = setting.getVoiceId();
                speechRate = setting.getSpeechRate();
                volume = setting.getVolume();
                pitch = setting.getPitch();
            }

            // 4.1 检查 volume 和 speechRate 是否为 0，如果是则设置默认值并更新到数据库
            boolean needUpdateSetting = false;
            
            // 检查 volume 是否为 0，如果是则设置为默认值 140
            if (volume != null && volume == 0) {
                volume = 140; // 默认值
                needUpdateSetting = true;
            }
            
            // 检查 speechRate 是否为 0，如果是则设置为默认值 100
            if (speechRate != null && speechRate == 0) {
                speechRate = 100; // 默认值
                needUpdateSetting = true;
            }
            
            // 如果表中没有记录，也需要创建一条带默认值的记录
            if (setting == null) {
                needUpdateSetting = true;
            }

            // 如果需要更新，将默认值保存到 synthesis_settings 表
            if (needUpdateSetting) {
                if (setting == null) {
                    // 如果表中没有记录，需要插入新记录
                    setting = new SynthesisSetting();
                    setting.setBreakingSentenceId(breakingSentenceId);
                }
                
                // 设置 volume 的默认值（如果为 null 或 0）
                if (volume == null || volume == 0) {
                    volume = 140;
                }
                setting.setVolume(volume);
                
                // 设置 speechRate 的默认值（如果为 null 或 0）
                if (speechRate == null || speechRate == 0) {
                    speechRate = 100;
                }
                setting.setSpeechRate(speechRate);
                
                // 设置 voiceId（如果为 null，使用默认值）
                if (setting.getVoiceId() == null || !StringUtils.hasText(setting.getVoiceId())) {
                    String finalVoiceId = (voiceId != null && StringUtils.hasText(voiceId)) ? voiceId : DEFAULT_VOICE_ID;
                    setting.setVoiceId(finalVoiceId);
                    // 同时更新局部变量，用于后续请求华为云
                    voiceId = finalVoiceId;
                }
                
                // 设置 pitch（如果为 null，使用默认值 0，因为数据库字段不允许 null）
                if (pitch == null) {
                    pitch = 0;
                }
                setting.setPitch(pitch);
                
                // 更新或插入记录
                synthesisSettingMapper.upsert(setting);
                logger.info("更新默认值到synthesis_settings表，breakingSentenceId: {}, volume: {}, speechRate: {}, voiceId: {}", 
                        breakingSentenceId, setting.getVolume(), setting.getSpeechRate(), setting.getVoiceId());
            }

            // 5. 构建TTS合成请求消息
            TtsSynthesisRequest synthesisRequest = new TtsSynthesisRequest();
            synthesisRequest.setBreakingSentenceId(breakingSentenceId);
            synthesisRequest.setVoiceId(voiceId);
            synthesisRequest.setSpeechRate(speechRate);
            synthesisRequest.setVolume(volume);
            synthesisRequest.setPitch(pitch);
            synthesisRequest.setResetStatus(false);
            synthesisRequest.setSsml(sentence.getSsml());  // 使用 SSML 字段
            
            // 6. 发送消息到RocketMQ，而不是直接调用华为云API
            //    实际的API调用会在 TtsSynthesisConsumer 中限流处理（5次/秒）
            boolean success = rocketMQTtsSynthesisService.sendSynthesisRequest(synthesisRequest);
            if (!success) {
                // 发送失败，更新状态为失败并返回"合成失败"
                logger.error("TTS合成请求发送失败，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
                return SynthesisStatus.Message.FAILED;
            }
            
            // 7. 更新状态为合成中（实际创建任务会在 Consumer 中完成）
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.PROCESSING, null, null);
            
            // 8. 返回合成状态文本（由于消息队列的存在，只会返回"合成中"或"合成失败"）
            return SynthesisStatus.Message.PROCESSING;
        } catch (Exception e) {
            // 任何异常都返回"合成失败"
            logger.error("合成断句时发生异常，breakingSentenceId: {}", breakingSentenceId, e);
            try {
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
            } catch (Exception ex) {
                logger.error("更新断句状态失败，breakingSentenceId: {}", breakingSentenceId, ex);
            }
            return SynthesisStatus.Message.FAILED;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String synthesizeOriginalSentence(Long originalSentenceId) {
        try {
            // 1. 参数验证：确保拆句ID不为空
            ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
            
            // 2. 查询该拆句下的所有断句
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByOriginalSentenceId(originalSentenceId);
            if (sentences.isEmpty()) {
                logger.warn("拆句下没有断句，originalSentenceId: {}", originalSentenceId);
                return "合成失败：拆句下没有断句";
            }
            
            // 3. 对每个断句调用合成逻辑，收集失败信息
            List<String> failureMessages = new ArrayList<>();
            for (BreakingSentence sentence : sentences) {
                try {
                    String result = synthesize(sentence.getBreakingSentenceId());
                    if (SynthesisStatus.Message.FAILED.equals(result)) {
                        // 查询失败原因
                        BreakingSentence failedSentence = breakingSentenceMapper.selectById(sentence.getBreakingSentenceId());
                        String errorMsg = "断句ID " + sentence.getBreakingSentenceId();
                        if (failedSentence == null) {
                            errorMsg += "：断句不存在";
                        } else if (!StringUtils.hasText(failedSentence.getSsml())) {
                            errorMsg += "：SSML为空";
                        } else {
                            errorMsg += "：TTS合成请求发送失败";
                        }
                        failureMessages.add(errorMsg);
                    }
                } catch (Exception e) {
                    logger.error("合成断句失败，breakingSentenceId: {}", sentence.getBreakingSentenceId(), e);
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                }
            }
            
            // 4. 如果有失败，返回失败信息；否则返回"合成中"
            if (!failureMessages.isEmpty()) {
                return SynthesisStatus.Message.FAILED + "：" + String.join("；", failureMessages);
            }
            return SynthesisStatus.Message.PROCESSING;
        } catch (Exception e) {
            logger.error("合成拆句时发生异常，originalSentenceId: {}", originalSentenceId, e);
            return SynthesisStatus.Message.FAILED + "：" + e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String synthesizeTask(Long taskId) {
        try {
            // 1. 参数验证：确保任务ID不为空
            ValidationUtil.notNull(taskId, "taskId不能为空");
            
            // 2. 验证任务是否存在
            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                logger.warn("任务不存在，taskId: {}", taskId);
                return SynthesisStatus.Message.FAILED + "：任务不存在";
            }
            
            // 3. 查询该任务下的所有断句
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
            if (sentences.isEmpty()) {
                logger.warn("任务下没有断句，taskId: {}", taskId);
                return SynthesisStatus.Message.FAILED + "：任务下没有断句";
            }
            
            // 4. 对每个断句调用合成逻辑，收集失败信息
            List<String> failureMessages = new ArrayList<>();
            for (BreakingSentence sentence : sentences) {
                try {
                    String result = synthesize(sentence.getBreakingSentenceId());
                    if (SynthesisStatus.Message.FAILED.equals(result)) {
                        // 查询失败原因
                        BreakingSentence failedSentence = breakingSentenceMapper.selectById(sentence.getBreakingSentenceId());
                        String errorMsg = "断句ID " + sentence.getBreakingSentenceId();
                        if (failedSentence == null) {
                            errorMsg += "：断句不存在";
                        } else if (!StringUtils.hasText(failedSentence.getSsml())) {
                            errorMsg += "：SSML为空";
                        } else {
                            errorMsg += "：TTS合成请求发送失败";
                        }
                        failureMessages.add(errorMsg);
                    }
                } catch (Exception e) {
                    logger.error("合成断句失败，breakingSentenceId: {}", sentence.getBreakingSentenceId(), e);
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                }
            }
            
            // 5. 如果有失败，返回失败信息；否则返回"合成中"
            if (!failureMessages.isEmpty()) {
                return SynthesisStatus.Message.FAILED + "：" + String.join("；", failureMessages);
            }
            return SynthesisStatus.Message.PROCESSING;
        } catch (Exception e) {
            logger.error("合成任务时发生异常，taskId: {}", taskId, e);
            return SynthesisStatus.Message.FAILED + "：" + e.getMessage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setConfig(SynthesisSetConfigRequest request) {
        synthesisConfigService.setConfig(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTtsCallback(TtsCallbackRequest callbackRequest) {
        ttsCallbackHandlerService.handleTtsCallback(callbackRequest);
    }

    @Override
    public SynthesisResultDTO getBreakingSentenceStatus(Long breakingSentenceId) {
        return synthesisStatusService.getBreakingSentenceStatus(breakingSentenceId);
    }

    @Override
    public OriginalSentenceSynthesisStatusDTO getOriginalSentenceStatus(Long originalSentenceId) {
        return synthesisStatusService.getOriginalSentenceStatus(originalSentenceId);
    }

    @Override
    public TaskSynthesisStatusDTO getTaskStatus(Long taskId) {
        return synthesisStatusService.getTaskStatus(taskId);
    }
}


