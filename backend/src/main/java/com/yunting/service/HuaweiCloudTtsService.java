package com.yunting.service;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.yunting.config.HuaweiCloudConfig;
import com.yunting.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * 华为云TTS语音合成服务
 * 参考文档: https://support.huaweicloud.com/api-metastudio/CreateAsyncTtsJob.html
 * 
 * 注意：此服务需要 huaweicloud-sdk-bundle 包中包含 MetaStudio 相关类
 * 如果编译失败，请检查 bundle 包版本或单独添加 huaweicloud-sdk-metastudio 依赖
 */
@Service
@ConditionalOnClass(name = "com.huaweicloud.sdk.metastudio.v1.MetaStudioClient")
public class HuaweiCloudTtsService {

    private static final Logger logger = LoggerFactory.getLogger(HuaweiCloudTtsService.class);

    @Autowired
    private ICredential credential;

    @Autowired
    private HuaweiCloudConfig config;

    @Value("${huaweicloud.project-id:}")
    private String projectId;

    // 默认合成参数
    private static final Integer DEFAULT_SPEED = 100;  // 正常语速
    private static final Integer DEFAULT_PITCH = 100; // 正常音高
    private static final Integer DEFAULT_VOLUME = 140; // 默认音量
    private static final String DEFAULT_AUDIO_FORMAT = "MP3"; // 默认音频格式

    // 任务查询重试配置
    private static final int MAX_RETRY_COUNT = 30; // 最大重试次数
    private static final long RETRY_INTERVAL_SECONDS = 2; // 重试间隔（秒）

    /**
     * 创建MetaStudio客户端（使用反射动态加载）
     */
    private Object createClient() {
        try {
            // 使用反射加载MetaStudioClient类
            Class<?> clientClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.MetaStudioClient");
            Class<?> regionClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.region.MetaStudioRegion");
            
            // 将区域字符串转换为MetaStudioRegion枚举值
            String regionStr = config.getRegion().toUpperCase().replace("-", "_");
            Object region;
            try {
                Method valueOf = regionClass.getMethod("valueOf", String.class);
                region = valueOf.invoke(null, regionStr);
            } catch (Exception e) {
                logger.warn("无法识别的区域: {}, 使用默认区域 CN_NORTH_4", regionStr);
                region = regionClass.getField("CN_NORTH_4").get(null);
            }
            
            // 创建客户端
            Method newBuilder = clientClass.getMethod("newBuilder");
            Object builder = newBuilder.invoke(null);
            Method withCredential = builder.getClass().getMethod("withCredential", ICredential.class);
            Method withRegion = builder.getClass().getMethod("withRegion", regionClass);
            Method build = builder.getClass().getMethod("build");
            
            withCredential.invoke(builder, credential);
            withRegion.invoke(builder, region);
            return build.invoke(builder);
            
        } catch (Exception e) {
            logger.error("创建MetaStudio客户端失败，请检查huaweicloud-sdk-bundle包是否包含MetaStudio服务", e);
            throw new BusinessException(10500, "MetaStudio SDK未找到，请检查依赖配置");
        }
    }

    /**
     * 创建TTS异步任务并等待完成，返回音频数据
     *
     * @param text 待合成文本
     * @param voiceAssetId 音色ID
     * @param speed 语速 (50-200，默认100)
     * @param pitch 音高 (50-200，默认100)
     * @param volume 音量 (90-240，默认140)
     * @return 音频文件的字节数组
     */
    public byte[] synthesize(String text, String voiceAssetId, Integer speed, Integer pitch, Integer volume) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(10400, "待合成文本不能为空");
        }
        if (!StringUtils.hasText(voiceAssetId)) {
            throw new BusinessException(10400, "音色ID不能为空");
        }

        Object client = createClient();
        String jobId = null;

        try {
            // 1. 创建异步TTS任务
            jobId = createAsyncTtsJob(client, text, voiceAssetId, speed, pitch, volume);
            logger.info("创建TTS异步任务成功，jobId: {}", jobId);

            // 2. 轮询查询任务状态，直到完成
            String audioUrl = waitForJobCompletion(client, jobId);
            if (audioUrl == null) {
                throw new BusinessException(10500, "TTS任务执行失败");
            }

            // 3. 下载音频文件
            logger.info("开始下载音频文件，URL: {}", audioUrl);
            byte[] audioData = downloadAudio(audioUrl);
            logger.info("音频文件下载成功，大小: {} bytes", audioData.length);

            return audioData;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("TTS合成失败，jobId: {}", jobId, e);
            throw new BusinessException(10500, "TTS合成失败: " + e.getMessage());
        }
    }

    /**
     * 创建异步TTS任务（使用反射）
     */
    private String createAsyncTtsJob(Object client, String text, String voiceAssetId,
                                     Integer speed, Integer pitch, Integer volume) {
        try {
            // 使用反射创建请求对象
            Class<?> requestClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.CreateAsyncTtsJobRequest");
            Class<?> bodyClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.CreateAsyncTtsJobRequestBody");
            
            Object request = requestClass.getDeclaredConstructor().newInstance();
            Object body = bodyClass.getDeclaredConstructor().newInstance();
            
            // 设置请求参数
            requestClass.getMethod("withProjectId", String.class).invoke(request, projectId);
            bodyClass.getMethod("withText", String.class).invoke(body, text);
            bodyClass.getMethod("withVoiceAssetId", String.class).invoke(body, voiceAssetId);
            bodyClass.getMethod("withSpeed", Integer.class).invoke(body, speed != null ? speed : DEFAULT_SPEED);
            bodyClass.getMethod("withPitch", Integer.class).invoke(body, pitch != null ? pitch : DEFAULT_PITCH);
            bodyClass.getMethod("withVolume", Integer.class).invoke(body, volume != null ? volume : DEFAULT_VOLUME);
            bodyClass.getMethod("withAudioFormat", String.class).invoke(body, DEFAULT_AUDIO_FORMAT);
            requestClass.getMethod("withBody", bodyClass).invoke(request, body);
            
            // 调用客户端方法
            Method createMethod = client.getClass().getMethod("createAsyncTtsJob", requestClass);
            Object response = createMethod.invoke(client, request);
            
            // 获取jobId
            Method getJobId = response.getClass().getMethod("getJobId");
            String jobId = (String) getJobId.invoke(response);
            
            if (jobId == null) {
                throw new BusinessException(10500, "创建TTS任务失败：未返回jobId");
            }
            return jobId;
            
        } catch (ServiceResponseException e) {
            logger.error("创建TTS任务失败: HTTP状态码={}, 错误码={}, 错误信息={}, 请求ID={}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg(), e.getRequestId());
            throw new BusinessException(10500, "创建TTS任务失败: " + e.getErrorMsg());
        } catch (Exception e) {
            logger.error("创建TTS任务异常", e);
            throw new BusinessException(10500, "创建TTS任务失败: " + e.getMessage());
        }
    }

    /**
     * 等待任务完成并返回音频URL（使用反射）
     */
    private String waitForJobCompletion(Object client, String jobId) {
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            try {
                // 使用反射创建请求对象
                Class<?> requestClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.ShowAsyncTtsJobRequest");
                Object request = requestClass.getDeclaredConstructor().newInstance();
                requestClass.getMethod("withProjectId", String.class).invoke(request, projectId);
                requestClass.getMethod("withJobId", String.class).invoke(request, jobId);
                
                // 调用客户端方法
                Method showMethod = client.getClass().getMethod("showAsyncTtsJob", requestClass);
                Object response = showMethod.invoke(client, request);
                
                // 获取响应体
                Method getBody = response.getClass().getMethod("getBody");
                Object body = getBody.invoke(response);

                if (body == null) {
                    logger.warn("查询TTS任务状态失败：响应体为空，jobId: {}", jobId);
                    sleep();
                    continue;
                }

                // 使用反射获取状态和URL
                Method getStatus = body.getClass().getMethod("getStatus");
                String status = (String) getStatus.invoke(body);
                logger.debug("TTS任务状态查询，jobId: {}, status: {}, 第{}次查询", jobId, status, i + 1);

                // 任务完成
                if ("SUCCEEDED".equals(status)) {
                    Method getAudioUrl = body.getClass().getMethod("getAudioUrl");
                    return (String) getAudioUrl.invoke(body);
                }

                // 任务失败
                if ("FAILED".equals(status)) {
                    Method getErrorMsg = body.getClass().getMethod("getErrorMsg");
                    String errorMsg = (String) getErrorMsg.invoke(body);
                    errorMsg = errorMsg != null ? errorMsg : "任务执行失败";
                    logger.error("TTS任务执行失败，jobId: {}, 错误信息: {}", jobId, errorMsg);
                    throw new BusinessException(10500, "TTS任务执行失败: " + errorMsg);
                }

                // 任务进行中，继续等待
                if ("PENDING".equals(status) || "PROCESSING".equals(status)) {
                    sleep();
                    continue;
                }

                // 未知状态
                logger.warn("TTS任务未知状态: {}, jobId: {}", status, jobId);
                sleep();

            } catch (ServiceResponseException e) {
                logger.error("查询TTS任务状态失败: HTTP状态码={}, 错误码={}, 错误信息={}, 请求ID={}",
                        e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg(), e.getRequestId());
                sleep();
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                logger.error("查询TTS任务状态异常，jobId: {}", jobId, e);
                sleep();
            }
        }

        logger.error("TTS任务超时，jobId: {}, 已重试{}次", jobId, MAX_RETRY_COUNT);
        throw new BusinessException(10500, "TTS任务执行超时");
    }

    /**
     * 下载音频文件
     */
    private byte[] downloadAudio(String audioUrl) {
        try {
            URL url = new URL(audioUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);   // 30秒读取超时

            try (InputStream inputStream = connection.getInputStream()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            logger.error("下载音频文件失败，URL: {}", audioUrl, e);
            throw new BusinessException(10500, "下载音频文件失败: " + e.getMessage());
        }
    }

    /**
     * 等待指定时间
     */
    private void sleep() {
        try {
            TimeUnit.SECONDS.sleep(RETRY_INTERVAL_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(10500, "任务等待被中断");
        }
    }
}

