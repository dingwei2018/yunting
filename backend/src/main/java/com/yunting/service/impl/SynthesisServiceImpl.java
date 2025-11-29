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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.service.ObsStorageService;
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

    // 存储job_id和breaking_sentence_id的映射关系
    // key: job_id, value: breaking_sentence_id
    private final Map<String, Long> jobIdMapping = new ConcurrentHashMap<>();

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

    public SynthesisServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                TaskMapper taskMapper,
                                SynthesisSettingMapper synthesisSettingMapper,
                                ObsStorageService obsStorageService) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
        this.synthesisSettingMapper = synthesisSettingMapper;
        this.obsStorageService = obsStorageService;
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

        // 5. 根据断句的字符数估算音频时长
        //    估算规则：每字符约120毫秒，最少1000毫秒
        int audioDuration = estimateDuration(sentence.getCharCount());
        
        // 6. 生成音频URL
        //    注意：当前为示例URL，实际生产环境应通过TTS服务生成真实的音频文件URL
        //    示例：使用配置的OBS前缀来构建URL
        //    String audioUrl = huaweiCloudObsPrefix + "breaking_" + sentence.getBreakingSentenceId() + ".mp3";
        String audioUrl = buildAudioUrl(sentence.getBreakingSentenceId());
        
        // 示例：在方法中使用配置参数
        // 例如：可以使用 huaweiCloudAk, huaweiCloudSk, huaweiCloudRegion, huaweiCloudProjectId 等
        // 这些参数已经从 application.properties 中自动注入，可以直接使用
        // System.out.println("使用区域: " + huaweiCloudRegion);
        // System.out.println("项目ID: " + huaweiCloudProjectId);

        //创建TTS异步任务
        ICredential auth = new BasicCredentials()
                .withProjectId(huaweiCloudProjectId)
                .withAk(huaweiCloudAk).withSk(huaweiCloudSk);
        MetaStudioClient client = MetaStudioClient.newBuilder()
                .withCredential(auth)
                .withRegion(MetaStudioRegion.valueOf(huaweiCloudRegion))
                .build();
        CreateAsyncTtsJobRequest request = new CreateAsyncTtsJobRequest();
        CreateAsyncTtsJobRequestBody body = new CreateAsyncTtsJobRequestBody();

        //参数设置
        body.withText(sentence.getContent())
                .withVoiceAssetId(voiceId)
                .withSpeed(speechRate)
                .withPitch(pitch)
                .withVolume(volume);
        request.withBody(body);
        try {
            CreateAsyncTtsJobResponse response = client.createAsyncTtsJob(request);
            String jobId = response.getJobId();
            
            if (StringUtils.hasText(jobId)) {
                // 保存job_id和breaking_sentence_id的映射关系
                jobIdMapping.put(jobId, breakingSentenceId);
                logger.info("创建TTS任务成功，jobId: {}, breakingSentenceId: {}", jobId, breakingSentenceId);
                
                // 更新状态为合成中
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 1, null, null);
            } else {
                logger.warn("创建TTS任务失败：未返回jobId");
                throw new BusinessException(10500, "创建TTS任务失败：未返回jobId");
            }
        } catch (ConnectionException e) {
            logger.error("创建TTS任务连接异常", e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "创建TTS任务连接异常: " + e.getMessage());
        } catch (RequestTimeoutException e) {
            logger.error("创建TTS任务请求超时", e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "创建TTS任务请求超时: " + e.getMessage());
        } catch (ServiceResponseException e) {
            logger.error("创建TTS任务服务响应异常: HTTP状态码={}, 错误码={}, 错误信息={}, 请求ID={}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg(), e.getRequestId());
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "创建TTS任务失败: " + e.getErrorMsg());
        } catch (BusinessException e) {
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw e;
        } catch (Exception e) {
            logger.error("创建TTS任务异常", e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "创建TTS任务异常: " + e.getMessage());
        }

        // 注意：此时任务已提交，等待回调通知
        // 不再直接更新为已完成状态，而是等待回调处理

        // 8. 构建并返回响应对象
        BreakingSentenceSynthesisResponseDTO responseDTO = new BreakingSentenceSynthesisResponseDTO();
        responseDTO.setBreakingSentenceId(breakingSentenceId);
        responseDTO.setTaskId(sentence.getTaskId());
        responseDTO.setSynthesisStatus(2); // 2表示已合成
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

        // 根据job_id查找对应的breaking_sentence_id
        Long breakingSentenceId = jobIdMapping.get(jobId);
        if (breakingSentenceId == null) {
            logger.warn("未找到job_id对应的断句ID，jobId: {}", jobId);
            return;
        }

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
        } finally {
            // 处理完成后，可以选择保留映射关系（用于查询）或删除
            // 这里保留映射关系，以便后续查询
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

        try {
            // 1. 生成OBS对象键
            String fileName = "breaking_" + breakingSentenceId + "_" + System.currentTimeMillis() + ".mp3";
            String objectKey = obsStorageService.buildObjectKey(fileName);

            // 2. 从下载URL下载文件并上传到OBS
            logger.info("开始下载并上传音频文件，downloadUrl: {}, objectKey: {}", audioDownloadUrl, objectKey);
            String obsUrl = obsStorageService.uploadFromUrl(audioDownloadUrl, objectKey);

            // 3. 转换音频时长（秒转毫秒）
            Integer audioDuration = audioDurationSeconds != null ? audioDurationSeconds * 1000 : null;

            // 4. 更新数据库
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 2, obsUrl, audioDuration);
            logger.info("TTS任务完成，已更新数据库，breakingSentenceId: {}, audioUrl: {}, duration: {}ms", 
                    breakingSentenceId, obsUrl, audioDuration);

        } catch (Exception e) {
            logger.error("处理完成回调异常，breakingSentenceId: {}", breakingSentenceId, e);
            breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, 3, null, null);
            throw new BusinessException(10500, "处理音频文件失败: " + e.getMessage());
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


