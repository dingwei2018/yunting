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
            if (setting == null) {
                logger.warn("断句的合成参数配置不存在，无法进行合成，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
                return SynthesisStatus.Message.FAILED;
            }
            
            String voiceId = setting.getVoiceId();
            Integer speechRate = setting.getSpeechRate();
            Integer volume = setting.getVolume();
            Integer pitch = setting.getPitch();
            
            // 验证必要参数是否存在
            if (!StringUtils.hasText(voiceId)) {
                logger.warn("断句的音色ID为空，无法进行合成，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
                return SynthesisStatus.Message.FAILED;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cancelSynthesis(Long breakingSentenceId) {
        try {
            // 1. 参数验证：确保断句ID不为空
            ValidationUtil.notNull(breakingSentenceId, "breakingSentenceId不能为空");
            
            // 2. 查询断句信息，验证断句是否存在
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence == null) {
                logger.warn("断句不存在，无法取消，breakingSentenceId: {}", breakingSentenceId);
                throw new com.yunting.exception.BusinessException(10404, "断句不存在");
            }
            
            // 3. 验证当前状态是否为PROCESSING
            Integer currentStatus = sentence.getSynthesisStatus();
            if (currentStatus == null || currentStatus != SynthesisStatus.Status.PROCESSING) {
                logger.warn("只能取消正在合成中的任务，当前状态: {}, breakingSentenceId: {}", 
                        currentStatus, breakingSentenceId);
                throw new com.yunting.exception.BusinessException(10400, "只能取消正在合成中的任务，当前状态: " + 
                        (currentStatus == null ? "未知" : getStatusText(currentStatus)));
            }
            
            // 4. 先清空jobId（如果存在），这样回调时查询不到记录就会忽略
            String originalJobId = sentence.getJobId();
            if (StringUtils.hasText(originalJobId)) {
                breakingSentenceMapper.updateJobId(breakingSentenceId, null);
            }
            
            // 5. 将状态重置为PENDING，清空audioUrl和audioDuration
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.PENDING, null, null);
            
            if (StringUtils.hasText(originalJobId)) {
                logger.info("已取消断句合成任务，已清空jobId，breakingSentenceId: {}, 原jobId: {}", 
                        breakingSentenceId, originalJobId);
            } else {
                logger.info("已取消断句合成任务，breakingSentenceId: {}", breakingSentenceId);
            }
            
            return "取消成功";
        } catch (com.yunting.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("取消断句合成任务时发生异常，breakingSentenceId: {}", breakingSentenceId, e);
            throw new com.yunting.exception.BusinessException(10500, "取消合成任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取状态文本描述
     */
    private String getStatusText(int status) {
        switch (status) {
            case 0:
                return "未合成";
            case 1:
                return "合成中";
            case 2:
                return "已合成";
            case 3:
                return "合成失败";
            default:
                return "未知状态(" + status + ")";
        }
    }
}


