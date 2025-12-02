package com.yunting.service.impl;

import com.huaweicloud.sdk.metastudio.v1.MetaStudioClient;
import com.yunting.dto.synthesis.BreakingSentenceSynthesisResponseDTO;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.TaskSynthesisBatchResponseDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.SynthesisSettingMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.SynthesisSetting;
import com.yunting.model.Task;
import com.yunting.service.SynthesisService;
import com.yunting.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.metastudio.v1.region.MetaStudioRegion;
import com.huaweicloud.sdk.metastudio.v1.*;
import com.huaweicloud.sdk.metastudio.v1.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.dto.synthesis.TtsSynthesisRequest;
import com.yunting.service.ObsStorageService;
import com.yunting.service.RocketMQTtsSynthesisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SynthesisServiceImpl implements SynthesisService {

    private static final Logger logger = LoggerFactory.getLogger(SynthesisServiceImpl.class);
    
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;
    private final SynthesisSettingMapper synthesisSettingMapper;
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
                                ObsStorageService obsStorageService,
                                RocketMQTtsSynthesisService rocketMQTtsSynthesisService) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.obsStorageService = obsStorageService;
        this.rocketMQTtsSynthesisService = rocketMQTtsSynthesisService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BreakingSentenceSynthesisResponseDTO synthesize(Long breakingSentenceId,
                                                           String voiceId,
                                                           Integer speechRate,
                                                           Integer volume,
                                                           Integer pitch,
                                                           boolean resetStatus) {
        // 1. 参数验证：确保断句ID不为空
        ValidationUtil.notNull(breakingSentenceId, "breaking_sentence_id不能为空");
        
        // 2. 查询断句信息，验证断句是否存在
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            throw new BusinessException(10404, "断句不存在");
        }

        // 3. 如果resetStatus为true，重置断句的合成状态为未合成(0)
        //    这允许对已合成的断句重新进行合成
        if (resetStatus) {
            breakingSentenceMapper.resetSynthesisStatus(breakingSentenceId);
            sentence.setSynthesisStatus(0);
        }

        // 4. 如果提供了任意合成参数，更新或创建该断句的合成设置
        //    合成设置会保存到synthesis_settings表中，供后续合成使用
        if (StringUtils.hasText(voiceId) || speechRate != null || volume != null || pitch != null) {
            upsertSetting(breakingSentenceId, voiceId, speechRate, volume, pitch);
        }

        // 5. 构建TTS合成请求消息
        TtsSynthesisRequest synthesisRequest = new TtsSynthesisRequest();
        synthesisRequest.setBreakingSentenceId(breakingSentenceId);
        synthesisRequest.setVoiceId(voiceId);
        synthesisRequest.setSpeechRate(speechRate);
        synthesisRequest.setVolume(volume);
        synthesisRequest.setPitch(pitch);
        synthesisRequest.setResetStatus(resetStatus);
        synthesisRequest.setContent(sentence.getContent());
        
        // 6. 发送消息到RocketMQ，而不是直接调用华为云API
        //    实际的API调用会在 TtsSynthesisConsumer 中限流处理（5次/秒）
        boolean success = rocketMQTtsSynthesisService.sendSynthesisRequest(synthesisRequest);
        if (!success) {
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "TTS合成请求发送失败");
        }
        
        // 7. 更新状态为合成中（实际创建任务会在 Consumer 中完成）
        breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 1, null, null);
        
        // 8. 构建并返回响应对象
        BreakingSentenceSynthesisResponseDTO responseDTO = new BreakingSentenceSynthesisResponseDTO();
        responseDTO.setBreakingSentenceId(breakingSentenceId);
        responseDTO.setTaskId(sentence.getTaskId());
        responseDTO.setSynthesisStatus(1); // 1表示合成中
        return responseDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskSynthesisBatchResponseDTO synthesizeBatch(Long taskId,
                                                         String voiceId,
                                                         Integer speechRate,
                                                         Integer volume,
                                                         Integer pitch,
                                                         List<Long> breakingSentenceIds) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }

        List<BreakingSentence> targets = resolveTargetSentences(taskId, breakingSentenceIds);
        if (targets.isEmpty()) {
            throw new BusinessException(10404, "未找到可合成的断句");
        }

        for (BreakingSentence sentence : targets) {
            if (StringUtils.hasText(voiceId) || speechRate != null || volume != null || pitch != null) {
                upsertSetting(sentence.getBreakingSentenceId(), voiceId, speechRate, volume, pitch);
            }
            int audioDuration = estimateDuration(sentence.getCharCount());
            String audioUrl = buildAudioUrl(sentence.getBreakingSentenceId());
            breakingSentenceMapper.updateSynthesisInfo(sentence.getBreakingSentenceId(), 2, audioUrl, audioDuration);
        }

        int total = targets.size();
        int pending = Math.max(0, breakingSentenceMapper.countByTaskId(taskId) - completedCount(taskId));

        TaskSynthesisBatchResponseDTO responseDTO = new TaskSynthesisBatchResponseDTO();
        responseDTO.setTaskId(taskId);
        responseDTO.setTotal(total);
        responseDTO.setPending(pending);
        return responseDTO;
    }

    @Override
    public TaskSynthesisStatusDTO getTaskSynthesisStatus(Long taskId) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        if (sentences.isEmpty()) {
            throw new BusinessException(10404, "暂无断句信息");
        }

        int total = sentences.size();
        int completed = (int) sentences.stream().filter(s -> Objects.equals(s.getSynthesisStatus(), 2)).count();
        int pending = total - completed;
        String status;
        if (completed == 0) {
            status = STATUS_PENDING;
        } else if (completed == total) {
            status = STATUS_COMPLETED;
        } else {
            status = STATUS_PROCESSING;
        }
        int progress = total == 0 ? 0 : (int) Math.floor((completed * 100.0) / total);

        SynthesisResultDTO resultDTO = sentences.stream()
                .filter(s -> s.getAudioUrl() != null)
                .reduce((first, second) -> second)
                .map(s -> {
                    SynthesisResultDTO dto = new SynthesisResultDTO();
                    dto.setSentenceId(s.getBreakingSentenceId());
                    dto.setAudioUrl(s.getAudioUrl());
                    dto.setAudioDuration(s.getAudioDuration());
                    return dto;
                })
                .orElse(null);

        TaskSynthesisStatusDTO dto = new TaskSynthesisStatusDTO();
        dto.setTaskId(taskId);
        dto.setStatus(status);
        dto.setProgress(progress);
        dto.setTotal(total);
        dto.setCompleted(completed);
        dto.setPending(pending);
        dto.setResult(resultDTO);
        return dto;
    }

    private void upsertSetting(Long breakingSentenceId,
                               String voiceId,
                               Integer speechRate,
                               Integer volume,
                               Integer pitch) {
        SynthesisSetting setting = new SynthesisSetting();
        setting.setBreakingSentenceId(breakingSentenceId);
        setting.setVoiceId(voiceId);
        setting.setVoiceName(null);
        setting.setSpeechRate(speechRate == null ? 0 : speechRate);
        setting.setVolume(volume == null ? 0 : volume);
        setting.setPitch(pitch == null ? 0 : pitch);
        synthesisSettingMapper.upsert(setting);
    }

    private List<BreakingSentence> resolveTargetSentences(Long taskId, List<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByIds(ids);
            if (sentences.size() != ids.size()) {
                throw new BusinessException(10404, "部分断句不存在");
            }
            boolean invalid = sentences.stream().anyMatch(s -> !Objects.equals(s.getTaskId(), taskId));
            if (invalid) {
                throw new BusinessException(10400, "断句不属于当前任务");
            }
            return sentences;
        }
        return breakingSentenceMapper.selectPendingByTaskId(taskId);
    }

    private int completedCount(Long taskId) {
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        return (int) sentences.stream().filter(s -> Objects.equals(s.getSynthesisStatus(), 2)).count();
    }

    private int estimateDuration(Integer charCount) {
        if (charCount == null || charCount <= 0) {
            return 1000;
        }
        return Math.max(1000, charCount * 120);
    }

    private String buildAudioUrl(Long breakingSentenceId) {
        return "https://example.com/audio/breaking_" + breakingSentenceId + ".mp3";
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

        } catch (Exception e) {
            logger.error("处理完成回调异常，breakingSentenceId: {}", breakingSentenceId, e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
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
    }
}


