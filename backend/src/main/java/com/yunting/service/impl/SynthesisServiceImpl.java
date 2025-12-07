package com.yunting.service.impl;

import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
import com.yunting.dto.synthesis.TtsSynthesisRequest;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.PauseSettingMapper;
import com.yunting.mapper.PolyphonicSettingMapper;
import com.yunting.mapper.ProsodySettingMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.PauseSetting;
import com.yunting.model.PolyphonicSetting;
import com.yunting.model.ProsodySetting;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.service.SynthesisService;
import com.yunting.service.ObsStorageService;
import com.yunting.service.RocketMQTtsSynthesisService;
import com.yunting.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SynthesisServiceImpl implements SynthesisService {

    private static final Logger logger = LoggerFactory.getLogger(SynthesisServiceImpl.class);

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
    private final PauseSettingMapper pauseSettingMapper;
    private final PolyphonicSettingMapper polyphonicSettingMapper;
    private final ProsodySettingMapper prosodySettingMapper;
    private final ObsStorageService obsStorageService;
    private final RocketMQTtsSynthesisService rocketMQTtsSynthesisService;

    // 从 application.properties 注入配置参数
    // 使用 @Value 注解，格式：${配置键名:默认值}
    // 如果配置文件中没有该键，则使用默认值；如果没有默认值且配置不存在，会抛出异常
    
    @Value("${huaweicloud.ak:}")
    private String huaweiCloudAk;

    @Value("${huaweicloud.sk:}")
    private String huaweiCloudSk;

    @Value("${huaweicloud.region:cn-north-4}")
    private String huaweiCloudRegion;

    @Value("${huaweicloud.project-id:}")
    private String huaweiCloudProjectId;

    @Value("${huaweicloud.obs.endpoint:}")
    private String huaweiCloudObsEndpoint;

    @Value("${huaweicloud.obs.bucket:}")
    private String huaweiCloudObsBucket;

    @Value("${huaweicloud.obs.prefix:audio/}")
    private String huaweiCloudObsPrefix;

    @Value("${file.storage.local.path:temp/audio}")
    private String localStoragePath;

    @Value("${app.callback.url:}")
    private String callbackUrl;

    public SynthesisServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                TaskMapper taskMapper,
                                SynthesisSettingMapper synthesisSettingMapper,
                                PauseSettingMapper pauseSettingMapper,
                                PolyphonicSettingMapper polyphonicSettingMapper,
                                ProsodySettingMapper prosodySettingMapper,
                                ObsStorageService obsStorageService,
                                RocketMQTtsSynthesisService rocketMQTtsSynthesisService) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.pauseSettingMapper = pauseSettingMapper;
        this.polyphonicSettingMapper = polyphonicSettingMapper;
        this.prosodySettingMapper = prosodySettingMapper;
        this.obsStorageService = obsStorageService;
        this.rocketMQTtsSynthesisService = rocketMQTtsSynthesisService;
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
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
                return "合成失败";
            }

            // 3. 验证 SSML 是否存在
            if (!StringUtils.hasText(sentence.getSsml())) {
                logger.warn("断句的SSML内容为空，无法进行合成，breakingSentenceId: {}", breakingSentenceId);
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
                return "合成失败";
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
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
                return "合成失败";
            }
            
            // 7. 更新状态为合成中（实际创建任务会在 Consumer 中完成）
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 1, null, null);
            
            // 8. 返回合成状态文本（由于消息队列的存在，只会返回"合成中"或"合成失败"）
            return "合成中";
        } catch (Exception e) {
            // 任何异常都返回"合成失败"
            logger.error("合成断句时发生异常，breakingSentenceId: {}", breakingSentenceId, e);
            try {
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            } catch (Exception ex) {
                logger.error("更新断句状态失败，breakingSentenceId: {}", breakingSentenceId, ex);
            }
            return "合成失败";
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
                    if ("合成失败".equals(result)) {
                        // 查询失败原因
                        BreakingSentence failedSentence = breakingSentenceMapper.selectById(sentence.getBreakingSentenceId());
                        String errorMsg = "断句ID " + sentence.getBreakingSentenceId();
                        if (failedSentence == null) {
                            errorMsg += "：断句不存在";
                        } else if (!StringUtils.hasText(failedSentence.getSsml())) {
                            errorMsg += "：SSML内容为空";
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
                return "合成失败：" + String.join("；", failureMessages);
            }
            return "合成中";
        } catch (Exception e) {
            logger.error("合成拆句时发生异常，originalSentenceId: {}", originalSentenceId, e);
            return "合成失败：" + e.getMessage();
        }
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
            boolean isNew = breakingSentenceId == -1L;

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
                newSentence.setSequence(maxSequence + 1);
                newSentence.setSynthesisStatus(0);
                
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

            // 2. 更新volume、voiceId、speed到synthesis_settings表
            if (config.getVolume() != null || StringUtils.hasText(config.getVoiceId()) || config.getSpeed() != null) {
                SynthesisSetting setting = synthesisSettingMapper.selectByBreakingSentenceId(breakingSentenceId);
                if (setting == null) {
                    setting = new SynthesisSetting();
                    setting.setBreakingSentenceId(breakingSentenceId);
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

            // 3. 更新breakList到pause_settings表（先删除旧的，再插入新的）
            pauseSettingMapper.deleteByBreakingSentenceId(breakingSentenceId);
            if (!CollectionUtils.isEmpty(config.getBreakList())) {
                List<PauseSetting> pauseSettings = new ArrayList<>();
                for (SynthesisSetConfigRequest.BreakConfig breakConfig : config.getBreakList()) {
                    PauseSetting pauseSetting = new PauseSetting();
                    pauseSetting.setBreakingSentenceId(breakingSentenceId);
                    pauseSetting.setPosition(breakConfig.getLocation());
                    pauseSetting.setDuration(breakConfig.getDuration());
                    pauseSetting.setType(1); // 1表示停顿
                    pauseSettings.add(pauseSetting);
                }
                if (!pauseSettings.isEmpty()) {
                    pauseSettingMapper.insertBatch(pauseSettings);
                }
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
                        if (phonemeConfig.getLocation() != null && phonemeConfig.getLocation() >= 0 
                                && phonemeConfig.getLocation() < content.length()
                                && StringUtils.hasText(phonemeConfig.getPh())) {
                            PolyphonicSetting polyphonicSetting = new PolyphonicSetting();
                            polyphonicSetting.setBreakingSentenceId(breakingSentenceId);
                            // 从content的location位置提取字符
                            String word = content.substring(phonemeConfig.getLocation(), 
                                    Math.min(phonemeConfig.getLocation() + 1, content.length()));
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



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTtsCallback(TtsCallbackRequest callbackRequest) {
        String jobId = callbackRequest.getJobId();
        String status = callbackRequest.getStatus();
        
        if (!StringUtils.hasText(jobId)) {
            logger.warn("回调请求中job_id为空，忽略处理");
            return;
        }

        // 根据job_id从数据库查找对应的breaking_sentence_id
        BreakingSentence sentence = breakingSentenceMapper.selectByJobId(jobId);
        if (sentence == null) {
            logger.warn("未找到job_id对应的断句，jobId: {}", jobId);
            return;
        }

        Long breakingSentenceId = sentence.getBreakingSentenceId();
        logger.info("处理TTS回调，jobId: {}, status: {}, breakingSentenceId: {}", jobId, status, breakingSentenceId);

        try {
            if ("FINISHED".equals(status)) {
                // 任务完成，处理音频文件
                handleFinishedCallback(callbackRequest, breakingSentenceId);
            } else if ("ERROR".equals(status)) {
                // 任务失败
                handleErrorCallback(callbackRequest, breakingSentenceId);
            } else if ("WAITING".equals(status)) {
                // 任务等待中，不需要处理
                logger.info("TTS任务等待中，jobId: {}", jobId);
            } else {
                logger.warn("未知的任务状态: {}, jobId: {}", status, jobId);
            }
        } catch (Exception e) {
            logger.error("处理TTS回调异常，jobId: {}, breakingSentenceId: {}", jobId, breakingSentenceId, e);
            // 更新状态为失败
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
        }
    }

    /**
     * 处理任务完成的回调
     */
    private void handleFinishedCallback(TtsCallbackRequest callbackRequest, Long breakingSentenceId) {
        String audioDownloadUrl = callbackRequest.getAudioFileDownloadUrl();
        Integer audioDurationSeconds = callbackRequest.getAudioDuration();

        if (!StringUtils.hasText(audioDownloadUrl)) {
            logger.warn("音频下载URL为空，jobId: {}, breakingSentenceId: {}", 
                    callbackRequest.getJobId(), breakingSentenceId);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            // 更新失败后也要检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                updateTaskStatusIfNeeded(sentence.getTaskId());
            }
            return;
        }

        File localFile = null;
        try {
            // 1. 生成本地文件路径
            String fileName = "breaking_" + breakingSentenceId + "_" + System.currentTimeMillis() + ".wav";
            Path localDir = Paths.get(localStoragePath);
            
            // 确保本地存储目录存在
            if (!Files.exists(localDir)) {
                Files.createDirectories(localDir);
                logger.info("创建本地存储目录: {}", localDir.toAbsolutePath());
            }
            
            localFile = localDir.resolve(fileName).toFile();

            // 2. 从下载URL下载文件到本地
            logger.info("开始从URL下载文件到本地，downloadUrl: {}, localFile: {}", 
                    audioDownloadUrl, localFile.getAbsolutePath());
            downloadFileToLocal(audioDownloadUrl, localFile);
            logger.info("文件下载成功，本地文件: {}, 文件大小: {} bytes", 
                    localFile.getAbsolutePath(), localFile.length());

            // 3. 生成OBS对象键
            String objectKey = obsStorageService.buildObjectKey(fileName);

            // 4. 从本地文件上传到OBS
            logger.info("开始从本地文件上传到OBS，localFile: {}, objectKey: {}", 
                    localFile.getAbsolutePath(), objectKey);
            String obsUrl = obsStorageService.uploadFromFile(localFile, objectKey);
            logger.info("文件上传到OBS成功，OBS URL: {}", obsUrl);

            // 5. 转换音频时长（秒转毫秒）
            Integer audioDuration = audioDurationSeconds != null ? 
                    audioDurationSeconds * 1000 : null;

            // 6. 更新数据库
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 2, obsUrl, audioDuration);
            logger.info("TTS任务完成，已更新数据库，breakingSentenceId: {}, audioUrl: {}, duration: {}ms", 
                    breakingSentenceId, obsUrl, audioDuration);

            // 7. 检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                updateTaskStatusIfNeeded(sentence.getTaskId());
            }

        } catch (Exception e) {
            logger.error("处理完成回调异常，breakingSentenceId: {}", breakingSentenceId, e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            // 更新失败后也要检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                updateTaskStatusIfNeeded(sentence.getTaskId());
            }
            throw new BusinessException(10500, "处理音频文件失败: " + e.getMessage());
        } finally {
            // 7. 清理临时文件
            if (localFile != null && localFile.exists()) {
                try {
                    boolean deleted = localFile.delete();
                    if (deleted) {
                        logger.info("已删除临时文件: {}", localFile.getAbsolutePath());
                    } else {
                        logger.warn("删除临时文件失败: {}", localFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("删除临时文件时发生异常: {}", localFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 从URL下载文件到本地
     * 
     * @param downloadUrl 下载URL
     * @param localFile 本地文件
     */
    private void downloadFileToLocal(String downloadUrl, File localFile) {
        try {
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(300000); // 5分钟读取超时
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(localFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                outputStream.flush();
                logger.info("文件下载完成，总大小: {} bytes", totalBytes);
            }
        } catch (Exception e) {
            logger.error("下载文件失败，URL: {}, localFile: {}", downloadUrl, 
                    localFile.getAbsolutePath(), e);
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理任务失败的回调
     */
    private void handleErrorCallback(TtsCallbackRequest callbackRequest, Long breakingSentenceId) {
        logger.error("TTS任务失败，jobId: {}, breakingSentenceId: {}", 
                callbackRequest.getJobId(), breakingSentenceId);
        breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
        
        // 检查并更新 task 状态
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence != null) {
            updateTaskStatusIfNeeded(sentence.getTaskId());
        }
    }

    /**
     * 根据 breaking_sentence 的合成状态更新 task 状态
     * 规则：
     * - 如果有 breaking_sentence 合成失败（状态为3），task 状态 = 3（失败）
     * - 如果有 breaking_sentence 还未完成合成（状态为0或1），task 状态 = 1（进行中）
     * - 如果全部 breaking_sentence 都已完成合成（状态都为2），task 状态 = 2（已完成）
     * 
     * @param taskId 任务ID
     */
    private void updateTaskStatusIfNeeded(Long taskId) {
        try {
            // 1. 查询该 task 下所有 breaking_sentence
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
            if (sentences.isEmpty()) {
                logger.warn("任务下没有断句，taskId: {}", taskId);
                return;
            }

            // 2. 统计各状态的数量
            int total = sentences.size();
            long completed = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), 2))
                    .count();
            long failed = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), 3))
                    .count();
            long processing = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), 1))
                    .count();
            long pending = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), 0))
                    .count();

            // 3. 根据规则判断并更新 task 状态（优先级：失败 > 进行中 > 已完成）
            Integer newStatus = null;
            if (failed > 0) {
                // 如果有失败的断句，task 状态 = 3（失败）
                newStatus = 3;
            } else if (processing > 0 || pending > 0) {
                // 如果有进行中或待处理的断句，task 状态 = 1（进行中）
                newStatus = 1;
            } else if (completed == total) {
                // 如果全部完成，task 状态 = 2（已完成）
                newStatus = 2;
            }

            // 4. 更新 task 状态
            if (newStatus != null) {
                taskMapper.updateStatus(taskId, newStatus);
                logger.info("更新任务状态，taskId: {}, newStatus: {}, total: {}, completed: {}, failed: {}, processing: {}, pending: {}", 
                        taskId, newStatus, total, completed, failed, processing, pending);
            }
        } catch (Exception e) {
            logger.error("更新任务状态失败，taskId: {}", taskId, e);
            // 不抛出异常，避免影响主流程
        }
    }
}


