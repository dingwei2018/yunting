package com.yunting.service.impl;

import com.yunting.dto.audio.AudioMergeMessage;
import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.AudioMergeMapper;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.AudioMerge;
import com.yunting.model.BreakingSentence;
import com.yunting.model.Task;
import com.yunting.service.AudioMergeService;
import com.yunting.service.FFmpegService;
import com.yunting.service.ObsStorageService;
import com.yunting.service.RocketMQAudioMergeService;
import com.yunting.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AudioMergeServiceImpl implements AudioMergeService {

    private static final Logger logger = LoggerFactory.getLogger(AudioMergeServiceImpl.class);

    private final AudioMergeMapper audioMergeMapper;
    private final TaskMapper taskMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;
    private final FFmpegService ffmpegService;
    private final ObsStorageService obsStorageService;
    private final RocketMQAudioMergeService rocketMQAudioMergeService;

    // 与 synthesize 接口共用临时目录配置
    @Value("${file.storage.local.path:temp/audio}")
    private String localStoragePath;

    public AudioMergeServiceImpl(AudioMergeMapper audioMergeMapper,
                                 TaskMapper taskMapper,
                                 BreakingSentenceMapper breakingSentenceMapper,
                                 FFmpegService ffmpegService,
                                 ObsStorageService obsStorageService,
                                 RocketMQAudioMergeService rocketMQAudioMergeService) {
        this.audioMergeMapper = audioMergeMapper;
        this.taskMapper = taskMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.ffmpegService = ffmpegService;
        this.obsStorageService = obsStorageService;
        this.rocketMQAudioMergeService = rocketMQAudioMergeService;
    }

    @Override
    public AudioMergeResponseDTO mergeAudio(Long taskId, AudioMergeRequest request) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        
        // 检查 FFmpeg 是否可用
        if (!ffmpegService.isFFmpegAvailable()) {
            throw new BusinessException(10500, "FFmpeg 不可用，无法合并音频");
        }
        
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        
        List<BreakingSentence> candidates = breakingSentenceMapper.selectByTaskId(taskId);
        if (CollectionUtils.isEmpty(candidates)) {
            throw new BusinessException(10404, "任务暂无断句");
        }

        List<Long> sentenceIds = request != null ? request.getSentenceIds() : null;
        List<BreakingSentence> toMerge;
        if (!CollectionUtils.isEmpty(sentenceIds)) {
            toMerge = candidates.stream()
                    .filter(bs -> sentenceIds.contains(bs.getBreakingSentenceId()))
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
            if (toMerge.size() != sentenceIds.size()) {
                throw new BusinessException(10404, "部分断句不存在");
            }
        } else {
            toMerge = candidates.stream()
                    .filter(bs -> bs.getAudioUrl() != null && StringUtils.hasText(bs.getAudioUrl()))
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
        }

        if (toMerge.isEmpty()) {
            throw new BusinessException(10404, "没有可合并的断句");
        }

        // 创建合并记录，状态为 processing
        // 注意：breaking_sentence_ids 会在实际合并后更新为实际合并的断句ID
        String mergedUrl = buildMergedAudioUrl(taskId);
        AudioMerge audioMerge = new AudioMerge();
        audioMerge.setTaskId(taskId);
        audioMerge.setBreakingSentenceIds(
                toMerge.stream()
                        .map(BreakingSentence::getBreakingSentenceId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
        audioMerge.setMergedAudioUrl(mergedUrl);
        audioMerge.setAudioDuration(0);
        audioMerge.setStatus(2); // processing
        audioMergeMapper.insert(audioMerge);

        // 发送消息到 RocketMQ，异步处理
        AudioMergeMessage mergeMessage = new AudioMergeMessage(
            taskId, 
            audioMerge.getMergeId(), 
            sentenceIds
        );
        
        boolean success = rocketMQAudioMergeService.sendAudioMergeMessage(mergeMessage);
        if (!success) {
            // 发送失败，更新状态为 failed
            updateMergeStatus(audioMerge.getMergeId(), 4);
            throw new BusinessException(10500, "发送合并任务到消息队列失败");
        }
        
        logger.info("音频合并任务已发送到消息队列，taskId: {}, mergeId: {}", taskId, audioMerge.getMergeId());
        return toResponse(audioMerge);
    }

    @Override
    public void processAudioMerge(AudioMergeMessage mergeMessage) {
        Long taskId = mergeMessage.getTaskId();
        Long mergeId = mergeMessage.getMergeId();
        List<Long> sentenceIds = mergeMessage.getSentenceIds();
        
        logger.info("开始处理音频合并，taskId: {}, mergeId: {}", taskId, mergeId);
        
        // 获取合并记录
        AudioMerge audioMerge = audioMergeMapper.selectById(mergeId);
        if (audioMerge == null) {
            logger.error("合并记录不存在，mergeId: {}", mergeId);
            return;
        }
        
        // 获取任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            logger.error("任务不存在，taskId: {}", taskId);
            updateMergeStatus(mergeId, 4);
            return;
        }
        
        // 获取断句列表
        List<BreakingSentence> candidates = breakingSentenceMapper.selectByTaskId(taskId);
        if (CollectionUtils.isEmpty(candidates)) {
            logger.error("任务暂无断句，taskId: {}", taskId);
            updateMergeStatus(mergeId, 4);
            return;
        }

        // 筛选要合并的断句
        List<BreakingSentence> toMerge;
        if (!CollectionUtils.isEmpty(sentenceIds)) {
            toMerge = candidates.stream()
                    .filter(bs -> sentenceIds.contains(bs.getBreakingSentenceId()))
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
        } else {
            toMerge = candidates.stream()
                    .filter(bs -> bs.getAudioUrl() != null && StringUtils.hasText(bs.getAudioUrl()))
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
        }

        if (toMerge.isEmpty()) {
            logger.error("没有可合并的断句，taskId: {}", taskId);
            updateMergeStatus(mergeId, 4);
            return;
        }

        // 执行音频合并
        List<File> tempInputFiles = new ArrayList<>();
        File tempOutputFile = null;
        
        try {
            // 1. 确保临时目录存在（与 synthesize 接口共用）
            Path localDir = Paths.get(localStoragePath);
            if (!Files.exists(localDir)) {
                Files.createDirectories(localDir);
                logger.info("创建本地存储目录: {}", localDir.toAbsolutePath());
            }
            
            // 2. 下载所有音频文件到本地临时目录（与 synthesize 接口共用）
            // 只下载有 audio_url 的断句，跳过没有音频的断句
            logger.info("开始下载音频文件，任务ID: {}, 断句数量: {}", taskId, toMerge.size());
            long timestamp = System.currentTimeMillis();
            List<BreakingSentence> validSentences = new ArrayList<>(); // 记录实际有音频的断句
            
            for (int i = 0; i < toMerge.size(); i++) {
                BreakingSentence bs = toMerge.get(i);
                String audioUrl = bs.getAudioUrl();
                
                // 跳过没有 audio_url 的断句
                if (!StringUtils.hasText(audioUrl)) {
                    logger.warn("跳过没有音频URL的断句，breakingSentenceId: {}, sequence: {}", 
                        bs.getBreakingSentenceId(), bs.getSequence());
                    continue;
                }
                
                // 使用 merge_ 前缀区分合并任务的临时文件
                String fileName = String.format("merge_input_%d_%d_%d_%d.wav", 
                    taskId, bs.getBreakingSentenceId(), validSentences.size(), timestamp);
                File tempFile = localDir.resolve(fileName).toFile();
                
                try {
                    downloadAudioFile(audioUrl, tempFile);
                    tempInputFiles.add(tempFile);
                    validSentences.add(bs);
                    logger.info("下载音频文件成功: {} -> {}", audioUrl, tempFile.getAbsolutePath());
                } catch (Exception e) {
                    // 下载失败时，记录日志但不抛出异常，继续处理其他断句
                    logger.warn("下载音频文件失败，跳过该断句，URL: {}, breakingSentenceId: {}, 错误: {}", 
                        audioUrl, bs.getBreakingSentenceId(), e.getMessage());
                }
            }
            
            // 检查是否有可合并的音频文件
            if (tempInputFiles.isEmpty()) {
                throw new BusinessException(10404, "没有可合并的音频文件（所有断句都没有音频或下载失败）");
            }
            
            logger.info("实际可合并的音频文件数量: {}/{}", tempInputFiles.size(), toMerge.size());
            
            // 3. 使用 FFmpeg 合并音频
            String outputFileName = String.format("merge_output_%d_%d.wav", taskId, timestamp);
            tempOutputFile = localDir.resolve(outputFileName).toFile();
            logger.info("开始合并音频，输出文件: {}", tempOutputFile.getAbsolutePath());
            
            int mergedDuration = ffmpegService.mergeAudioFiles(tempInputFiles, tempOutputFile);
            logger.info("音频合并成功，时长: {}ms", mergedDuration);
            
            // 4. 上传合并后的音频到 OBS
            String obsFileName = "task_" + taskId + "_merged_" + timestamp + ".wav";
            String objectKey = obsStorageService.buildObjectKey(obsFileName);
            String mergedUrl = obsStorageService.uploadFromFile(tempOutputFile, objectKey);
            logger.info("合并音频上传到OBS成功: {}", mergedUrl);
            
            // 5. 更新合并记录
            // 更新为实际合并的断句ID列表
            String actualSentenceIds = validSentences.stream()
                    .map(BreakingSentence::getBreakingSentenceId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            
            audioMerge.setBreakingSentenceIds(actualSentenceIds);
            audioMerge.setMergedAudioUrl(mergedUrl);
            audioMerge.setAudioDuration(mergedDuration);
            audioMerge.setStatus(3); // completed
            audioMergeMapper.updateById(audioMerge);
            
            // 6. 更新任务信息
            task.setMergedAudioUrl(mergedUrl);
            task.setMergedAudioDuration(mergedDuration);
            taskMapper.updateById(task);
            
            logger.info("音频合并处理完成，taskId: {}, mergeId: {}", taskId, mergeId);
            
        } catch (Exception e) {
            logger.error("合并音频失败，任务ID: {}, mergeId: {}", taskId, mergeId, e);
            updateMergeStatus(mergeId, 4); // failed
        } finally {
            // 清理临时文件
            cleanupTempFiles(tempInputFiles);
            if (tempOutputFile != null && tempOutputFile.exists()) {
                try {
                    Files.delete(tempOutputFile.toPath());
                    logger.info("已删除临时输出文件: {}", tempOutputFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("删除临时输出文件失败: {}", tempOutputFile.getAbsolutePath(), e);
                }
            }
        }
    }

    @Override
    public AudioMergeResponseDTO getMergeStatus(Long mergeId) {
        ValidationUtil.notNull(mergeId, "mergeid不能为空");
        AudioMerge audioMerge = audioMergeMapper.selectById(mergeId);
        if (audioMerge == null) {
            throw new BusinessException(10404, "合并任务不存在");
        }
        return toResponse(audioMerge);
    }

    private AudioMergeResponseDTO toResponse(AudioMerge audioMerge) {
        AudioMergeResponseDTO dto = new AudioMergeResponseDTO();
        dto.setMergeId(audioMerge.getMergeId());
        dto.setTaskId(audioMerge.getTaskId());
        dto.setMergedAudioUrl(audioMerge.getMergedAudioUrl());
        dto.setAudioDuration(audioMerge.getAudioDuration());
        dto.setStatus(mapStatus(audioMerge.getStatus()));
        return dto;
    }

    private String mapStatus(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 1 -> "pending";
            case 2 -> "processing";
            case 3 -> "completed";
            case 4 -> "failed";
            default -> "unknown";
        };
    }

    /**
     * 下载音频文件到本地
     * 与 synthesize 接口使用相同的下载逻辑
     */
    private void downloadAudioFile(String audioUrl, File localFile) {
        try {
            URL url = new URL(audioUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(300000); // 5分钟读取超时
            
            try (var inputStream = connection.getInputStream();
                 var outputStream = Files.newOutputStream(localFile.toPath())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                outputStream.flush();
                logger.debug("文件下载完成，总大小: {} bytes", totalBytes);
            }
        } catch (Exception e) {
            logger.error("下载音频文件失败，URL: {}, localFile: {}", audioUrl, 
                localFile.getAbsolutePath(), e);
            throw new RuntimeException("下载音频文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(List<File> tempFiles) {
        if (tempFiles == null || tempFiles.isEmpty()) {
            return;
        }
        
        for (File file : tempFiles) {
            if (file != null && file.exists()) {
                try {
                    Files.delete(file.toPath());
                    logger.debug("已删除临时文件: {}", file.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("删除临时文件失败: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 更新合并状态
     */
    private void updateMergeStatus(Long mergeId, Integer status) {
        try {
            AudioMerge audioMerge = audioMergeMapper.selectById(mergeId);
            if (audioMerge != null) {
                audioMerge.setStatus(status);
                audioMergeMapper.updateById(audioMerge);
            }
        } catch (Exception e) {
            logger.error("更新合并状态失败，mergeId: {}", mergeId, e);
        }
    }

    private String buildMergedAudioUrl(Long taskId) {
        // 临时URL，实际URL会在上传OBS后更新
        return "https://example.com/audio/task_" + taskId + "_merged.wav";
    }
}


