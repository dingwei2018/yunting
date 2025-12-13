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
import com.yunting.constant.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /**
     * 取消断句合成任务（内部方法，不更新任务状态）
     * 只负责取消单个断句的合成任务，不包含任务状态更新逻辑
     * 
     * @param breakingSentenceId 断句ID
     * @return 取消的断句信息，包含任务ID
     * @throws com.yunting.exception.BusinessException 如果断句不存在或状态不正确
     */
    private BreakingSentence cancelSynthesisInternal(Long breakingSentenceId) {
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
        
        return sentence;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cancelSynthesis(Long breakingSentenceId) {
        try {
            // 调用内部方法取消断句
            BreakingSentence sentence = cancelSynthesisInternal(breakingSentenceId);
            
            // 更新任务状态
            Long taskId = sentence.getTaskId();
            if (taskId != null) {
                try {
                    updateTaskStatusAfterCancel(taskId);
                } catch (Exception e) {
                    // 更新任务状态失败不影响取消操作的主流程，只记录日志
                    logger.warn("取消断句后更新任务状态失败，taskId: {}, breakingSentenceId: {}", 
                            taskId, breakingSentenceId, e);
                }
            }
            
            return "取消成功";
        } catch (com.yunting.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("取消断句合成任务时发生异常，breakingSentenceId: {}", breakingSentenceId, e);
            throw new com.yunting.exception.BusinessException(10500, "取消合成任务失败: " + e.getMessage());
        }
    }
    
    @Override
    public String cancelOriginalSentence(Long originalSentenceId) {
        try {
            // 1. 参数验证：确保拆句ID不为空
            ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
            
            // 2. 查询该拆句下的所有断句
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByOriginalSentenceId(originalSentenceId);
            if (sentences.isEmpty()) {
                logger.warn("拆句下没有断句，无法取消，originalSentenceId: {}", originalSentenceId);
                return "拆句下没有断句";
            }
            
            // 3. 过滤出状态为PROCESSING的断句
            List<BreakingSentence> processingSentences = sentences.stream()
                    .filter(s -> s.getSynthesisStatus() != null && 
                            s.getSynthesisStatus() == SynthesisStatus.Status.PROCESSING)
                    .collect(java.util.stream.Collectors.toList());
            
            if (processingSentences.isEmpty()) {
                logger.info("拆句下没有正在合成中的断句，originalSentenceId: {}", originalSentenceId);
                return "拆句下没有正在合成中的断句";
            }
            
            // 4. 对每个PROCESSING状态的断句调用cancelSynthesisInternal，收集结果
            int successCount = 0;
            int failCount = 0;
            List<String> failureMessages = new ArrayList<>();
            Long taskId = null;  // 用于最后更新任务状态（所有断句属于同一个任务）
            
            for (BreakingSentence sentence : processingSentences) {
                try {
                    BreakingSentence cancelledSentence = cancelSynthesisInternal(sentence.getBreakingSentenceId());
                    // 保存任务ID（所有断句属于同一个任务，所以只需要保存一次）
                    if (taskId == null) {
                        taskId = cancelledSentence.getTaskId();
                    }
                    successCount++;
                } catch (com.yunting.exception.BusinessException e) {
                    failCount++;
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                    logger.warn("取消断句失败，breakingSentenceId: {}, 错误: {}", 
                            sentence.getBreakingSentenceId(), e.getMessage());
                } catch (Exception e) {
                    failCount++;
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                    logger.error("取消断句时发生异常，breakingSentenceId: {}", 
                            sentence.getBreakingSentenceId(), e);
                }
            }
            
            // 5. 统一更新任务状态（只在有成功取消的断句时更新）
            if (successCount > 0 && taskId != null) {
                try {
                    updateTaskStatusAfterCancel(taskId);
                } catch (Exception e) {
                    // 更新任务状态失败不影响取消操作的主流程，只记录日志
                    logger.warn("取消拆句后更新任务状态失败，taskId: {}, originalSentenceId: {}", 
                            taskId, originalSentenceId, e);
                }
            }
            
            // 6. 返回汇总信息
            String result = String.format("已取消%d个断句", successCount);
            if (failCount > 0) {
                result += String.format("，%d个失败：%s", failCount, String.join("；", failureMessages));
            }
            logger.info("取消拆句合成任务完成，originalSentenceId: {}, 成功: {}, 失败: {}", 
                    originalSentenceId, successCount, failCount);
            return result;
        } catch (Exception e) {
            logger.error("取消拆句合成任务时发生异常，originalSentenceId: {}", originalSentenceId, e);
            throw new com.yunting.exception.BusinessException(10500, "取消拆句合成任务失败: " + e.getMessage());
        }
    }

    @Override
    public String cancelTask(Long taskId) {
        try {
            // 1. 参数验证：确保任务ID不为空
            ValidationUtil.notNull(taskId, "taskId不能为空");
            
            // 2. 验证任务是否存在
            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                logger.warn("任务不存在，无法取消，taskId: {}", taskId);
                throw new com.yunting.exception.BusinessException(10404, "任务不存在");
            }
            
            // 3. 查询该任务下的所有断句
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
            if (sentences.isEmpty()) {
                logger.warn("任务下没有断句，无法取消，taskId: {}", taskId);
                return "任务下没有断句";
            }
            
            // 4. 过滤出状态为PROCESSING的断句
            List<BreakingSentence> processingSentences = sentences.stream()
                    .filter(s -> s.getSynthesisStatus() != null && 
                            s.getSynthesisStatus() == SynthesisStatus.Status.PROCESSING)
                    .collect(java.util.stream.Collectors.toList());
            
            if (processingSentences.isEmpty()) {
                logger.info("任务下没有正在合成中的断句，taskId: {}", taskId);
                return "任务下没有正在合成中的断句";
            }
            
            // 5. 对每个PROCESSING状态的断句调用cancelSynthesisInternal，收集结果
            int successCount = 0;
            int failCount = 0;
            List<String> failureMessages = new ArrayList<>();
            
            for (BreakingSentence sentence : processingSentences) {
                try {
                    cancelSynthesisInternal(sentence.getBreakingSentenceId());
                    successCount++;
                } catch (com.yunting.exception.BusinessException e) {
                    failCount++;
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                    logger.warn("取消断句失败，breakingSentenceId: {}, 错误: {}", 
                            sentence.getBreakingSentenceId(), e.getMessage());
                } catch (Exception e) {
                    failCount++;
                    failureMessages.add("断句ID " + sentence.getBreakingSentenceId() + "：" + e.getMessage());
                    logger.error("取消断句时发生异常，breakingSentenceId: {}", 
                            sentence.getBreakingSentenceId(), e);
                }
            }
            
            // 6. 统一更新任务状态（只在有成功取消的断句时更新）
            if (successCount > 0) {
                try {
                    updateTaskStatusAfterCancel(taskId);
                } catch (Exception e) {
                    // 更新任务状态失败不影响取消操作的主流程，只记录日志
                    logger.warn("取消任务后更新任务状态失败，taskId: {}", taskId, e);
                }
            }
            
            // 7. 返回汇总信息
            String result = String.format("已取消%d个断句", successCount);
            if (failCount > 0) {
                result += String.format("，%d个失败：%s", failCount, String.join("；", failureMessages));
            }
            logger.info("取消任务合成任务完成，taskId: {}, 成功: {}, 失败: {}", 
                    taskId, successCount, failCount);
            return result;
        } catch (com.yunting.exception.BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("取消任务合成任务时发生异常，taskId: {}", taskId, e);
            throw new com.yunting.exception.BusinessException(10500, "取消任务合成任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消断句后更新任务状态
     * 根据任务下所有断句的状态更新任务状态，优先级：
     * 1. 如果有失败的断句（状态为3）→ 更新为 SYNTHESIS_FAILED
     * 2. 如果有合成中的断句（状态为1）→ 更新为 SYNTHESIS_PROCESSING
     * 3. 否则 → 更新为 BREAKING_COMPLETED
     * 
     * @param taskId 任务ID
     */
    private void updateTaskStatusAfterCancel(Long taskId) {
        if (taskId == null) {
            logger.warn("任务ID为空，无法更新任务状态");
            return;
        }
        
        // 查询该任务下的所有断句
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        if (sentences.isEmpty()) {
            logger.warn("任务下没有断句，无法更新任务状态，taskId: {}", taskId);
            return;
        }
        
        // 统计各状态的数量
        long failed = sentences.stream()
                .filter(s -> s.getSynthesisStatus() != null && 
                        Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.FAILED))
                .count();
        long processing = sentences.stream()
                .filter(s -> s.getSynthesisStatus() != null && 
                        Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PROCESSING))
                .count();
        
        // 根据优先级判断并更新任务状态
        Integer newStatus = null;
        if (failed > 0) {
            // 优先级1：如果有失败的断句，任务状态 = SYNTHESIS_FAILED（语音合成失败）
            newStatus = TaskStatus.Status.SYNTHESIS_FAILED;
        } else if (processing > 0) {
            // 优先级2：如果有合成中的断句，任务状态 = SYNTHESIS_PROCESSING（语音合成中）
            newStatus = TaskStatus.Status.SYNTHESIS_PROCESSING;
        } else {
            // 优先级3：否则，任务状态 = BREAKING_COMPLETED（拆句完成）
            newStatus = TaskStatus.Status.BREAKING_COMPLETED;
        }
        
        // 更新任务状态
        if (newStatus != null) {
            taskMapper.updateStatus(taskId, newStatus);
            logger.info("取消断句后更新任务状态，taskId: {}, newStatus: {}, failed: {}, processing: {}", 
                    taskId, newStatus, failed, processing);
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


