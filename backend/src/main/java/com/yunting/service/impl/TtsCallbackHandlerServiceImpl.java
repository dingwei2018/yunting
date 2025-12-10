package com.yunting.service.impl;

import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.service.ObsStorageService;
import com.yunting.service.TaskStatusUpdateService;
import com.yunting.service.TtsCallbackHandlerService;
import com.yunting.constant.SynthesisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TTS回调处理服务实现
 */
@Service
public class TtsCallbackHandlerServiceImpl implements TtsCallbackHandlerService {

    private static final Logger logger = LoggerFactory.getLogger(TtsCallbackHandlerServiceImpl.class);

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final ObsStorageService obsStorageService;
    private final TaskStatusUpdateService taskStatusUpdateService;

    @Value("${file.storage.local.path:temp/audio}")
    private String localStoragePath;

    public TtsCallbackHandlerServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                         ObsStorageService obsStorageService,
                                         TaskStatusUpdateService taskStatusUpdateService) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.obsStorageService = obsStorageService;
        this.taskStatusUpdateService = taskStatusUpdateService;
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
            if (SynthesisStatus.Callback.FINISHED.equals(status)) {
                // 任务完成，处理音频文件
                handleFinishedCallback(callbackRequest, breakingSentenceId);
            } else if (SynthesisStatus.Callback.ERROR.equals(status)) {
                // 任务失败
                handleErrorCallback(callbackRequest, breakingSentenceId);
            } else if (SynthesisStatus.Callback.WAITING.equals(status)) {
                // 任务等待中，不需要处理
                logger.info("TTS任务等待中，jobId: {}", jobId);
            } else {
                logger.warn("未知的任务状态: {}, jobId: {}", status, jobId);
            }
        } catch (Exception e) {
            logger.error("处理TTS回调异常，jobId: {}, breakingSentenceId: {}", jobId, breakingSentenceId, e);
            // 更新状态为失败
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
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
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
            // 更新失败后也要检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                taskStatusUpdateService.updateTaskStatusIfNeeded(sentence.getTaskId());
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
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.COMPLETED, obsUrl, audioDuration);
            logger.info("TTS任务完成，已更新数据库，breakingSentenceId: {}, audioUrl: {}, duration: {}ms", 
                    breakingSentenceId, obsUrl, audioDuration);

            // 7. 检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                taskStatusUpdateService.updateTaskStatusIfNeeded(sentence.getTaskId());
            }

        } catch (Exception e) {
            logger.error("处理完成回调异常，breakingSentenceId: {}", breakingSentenceId, e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
            // 更新失败后也要检查并更新 task 状态
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence != null) {
                taskStatusUpdateService.updateTaskStatusIfNeeded(sentence.getTaskId());
            }
            throw new BusinessException(10500, "处理音频文件失败: " + e.getMessage());
        } finally {
            // 清理临时文件
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
        breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, SynthesisStatus.Status.FAILED, null, null);
        
        // 检查并更新 task 状态
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence != null) {
            taskStatusUpdateService.updateTaskStatusIfNeeded(sentence.getTaskId());
        }
    }
}
