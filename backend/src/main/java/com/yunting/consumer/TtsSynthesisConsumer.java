package com.yunting.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.metastudio.v1.MetaStudioClient;
import com.huaweicloud.sdk.metastudio.v1.model.*;
import com.huaweicloud.sdk.metastudio.v1.region.MetaStudioRegion;
import com.yunting.config.RocketMQConfig;
import com.yunting.constant.SynthesisStatus;
import com.yunting.dto.synthesis.TtsSynthesisRequest;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.service.TtsSynthesisCoordinator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TTS合成请求消费者
 * 从RocketMQ消费TTS合成请求，限流5次/秒调用华为云API
 * 支持顺序消息消费，确保所有合成请求按顺序执行
 */
@Component
public class TtsSynthesisConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsSynthesisConsumer.class);
    
    private PushConsumer consumer;
    private final BreakingSentenceMapper breakingSentenceMapper;
    private final ObjectMapper objectMapper;
    private final RocketMQConfig rocketMQConfig;
    private final TtsSynthesisCoordinator ttsSynthesisCoordinator;
    private final TransactionTemplate transactionTemplate;
    private final BlockingQueue<MessageView> messageQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService rateLimiterExecutor = Executors.newScheduledThreadPool(1);
    private volatile boolean running = false;
    
    // 限流配置：5次/秒 = 每200毫秒处理一次
    private static final long RATE_LIMIT_INTERVAL_MS = 200;
    
    // 华为云配置
    @Value("${huaweicloud.ak:}")
    private String huaweiCloudAk;
    
    @Value("${huaweicloud.sk:}")
    private String huaweiCloudSk;
    
    @Value("${huaweicloud.region:cn-north-4}")
    private String huaweiCloudRegion;
    
    @Value("${huaweicloud.project-id:}")
    private String huaweiCloudProjectId;
    
    @Value("${app.callback.url:}")
    private String callbackUrl;
    
    public TtsSynthesisConsumer(BreakingSentenceMapper breakingSentenceMapper,
                                ObjectMapper objectMapper,
                                RocketMQConfig rocketMQConfig,
                                TtsSynthesisCoordinator ttsSynthesisCoordinator,
                                PlatformTransactionManager transactionManager) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.objectMapper = objectMapper;
        this.rocketMQConfig = rocketMQConfig;
        this.ttsSynthesisCoordinator = ttsSynthesisCoordinator;
        // 创建事务模板，配置为遇到任何异常都回滚
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    
    @PostConstruct
    public void startConsuming() {
        logger.info("TTS合成请求消费者启动");
        
        // 启动限流处理线程
        running = true;
        rateLimiterExecutor.scheduleAtFixedRate(this::processNextMessage, 
                0, RATE_LIMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        // 在后台线程中创建 Consumer
        new Thread(() -> {
            int maxRetries = 10;
            int retryInterval = 5000;
            
            for (int i = 0; i < maxRetries; i++) {
                try {
                    ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                            .setEndpoints(rocketMQConfig.getProxyEndpoint());
                    ClientConfiguration configuration = builder.build();
                    
                    // 订阅 TTS_SYNTHESIS Tag
                    FilterExpression filterExpression = new FilterExpression("TTS_SYNTHESIS", FilterExpressionType.TAG);
                    
                    MessageListener messageListener = new MessageListener() {
                        @Override
                        public ConsumeResult consume(MessageView messageView) {
                            // 将消息放入队列，由限流线程处理
                            messageQueue.offer(messageView);
                            return ConsumeResult.SUCCESS; // 先确认消息，避免重复
                        }
                    };
                    
                    consumer = rocketMQConfig.getProvider().newPushConsumerBuilder()
                            .setClientConfiguration(configuration)
                            .setConsumerGroup(rocketMQConfig.getTtsSynthesisConsumerGroup())
                            .setSubscriptionExpressions(Collections.singletonMap(
                                    rocketMQConfig.getTtsTopic(), filterExpression))
                            .setConsumptionThreadCount(1)  // 单线程消费，保证顺序消息的顺序处理
                            .setMessageListener(messageListener)
                            .build();
                    
                    logger.info("TTS合成请求监听器注册成功（顺序消息模式），Topic: {}, ConsumerGroup: {}, 消费线程数: 1", 
                            rocketMQConfig.getTtsTopic(), 
                            rocketMQConfig.getTtsSynthesisConsumerGroup());
                    return;
                } catch (Exception e) {
                    if (i < maxRetries - 1) {
                        logger.warn("注册消息监听器失败（尝试 {}/{}），将在 {} 秒后重试。错误: {}", 
                                i + 1, maxRetries, retryInterval / 1000, e.getMessage());
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else {
                        logger.error("注册消息监听器失败，已达到最大重试次数（{}），Consumer 将无法启动", maxRetries, e);
                    }
                }
            }
        }, "TtsSynthesisConsumer-Startup-Thread").start();
    }
    
    /**
     * 限流处理：每200ms处理一条消息（5次/秒）
     */
    private void processNextMessage() {
        if (!running) {
            return;
        }
        
        MessageView messageView = messageQueue.poll();
        if (messageView != null) {
            processMessage(messageView);
        }
    }
    
    /**
     * 处理单条消息
     */
    private void processMessage(MessageView messageView) {
        try {
            ByteBuffer byteBuffer = messageView.getBody();
            byte[] body = new byte[byteBuffer.remaining()];
            byteBuffer.get(body);
            
            TtsSynthesisRequest request = objectMapper.readValue(body, TtsSynthesisRequest.class);
            Long breakingSentenceId = request.getBreakingSentenceId();
            
            logger.info("处理TTS合成请求，breakingSentenceId: {}, 线程: {}", 
                    breakingSentenceId, Thread.currentThread().getName());
            
            // 在设置合成参数前，先检查断句状态
            // 如果状态不是PROCESSING，说明任务已被取消、已完成或未开始，应该跳过处理
            BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
            if (sentence == null) {
                logger.warn("断句不存在，跳过处理，breakingSentenceId: {}", breakingSentenceId);
                return;
            }
            
            Integer currentStatus = sentence.getSynthesisStatus();
            if (currentStatus == null || !currentStatus.equals(SynthesisStatus.Status.PROCESSING)) {
                logger.info("断句状态不是PROCESSING，跳过处理，breakingSentenceId: {}, 当前状态: {}", 
                        breakingSentenceId, currentStatus);
                return;
            }
            
            // 确保阅读规则已同步，然后执行合成
            ttsSynthesisCoordinator.ensureVocabularyConfigsAndSynthesize(request, () -> {
                createTtsJobInternal(request);
            });
            
        } catch (Exception e) {
            logger.error("处理TTS合成请求失败", e);
        }
    }
    
    /**
     * 创建华为云TTS任务（内部方法，由协调器调用）
     * 使用编程式事务确保数据库更新操作的原子性
     */
    private void createTtsJobInternal(TtsSynthesisRequest request) {
        Long breakingSentenceId = request.getBreakingSentenceId();
        String jobId = null;
        
        try {
            // 创建华为云客户端
            ICredential auth = new BasicCredentials()
                    .withProjectId(huaweiCloudProjectId)
                    .withAk(huaweiCloudAk)
                    .withSk(huaweiCloudSk);
            
            MetaStudioClient client = MetaStudioClient.newBuilder()
                    .withCredential(auth)
                    .withRegion(MetaStudioRegion.valueOf(huaweiCloudRegion))
                    .build();
            
            // 构建请求
            CreateAsyncTtsJobRequest ttsRequest = new CreateAsyncTtsJobRequest();
            CreateAsyncTtsJobRequestBody body = new CreateAsyncTtsJobRequestBody();
            
            // 获取文本内容：使用 SSML（SSML 有默认值，不会为空）
            String textContent = request.getSsml();
            if (!StringUtils.hasText(textContent)) {
                logger.warn("SSML为空，无法进行合成，breakingSentenceId: {}", breakingSentenceId);
                // 在事务中更新状态为失败
                updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
                return;
            }
            
            // 使用文本内容（SSML）
            body.withText(textContent)
                    .withVoiceAssetId(request.getVoiceId())
                    .withSpeed(request.getSpeechRate())
                    .withVolume(request.getVolume())
                    .withCallbackConfig(new TtsCallBackConfig().withCallbackUrl(callbackUrl));
            
            ttsRequest.withBody(body);
            
            // 调用华为云 API
            CreateAsyncTtsJobResponse response = client.createAsyncTtsJob(ttsRequest);
            jobId = response.getJobId();
            
            if (StringUtils.hasText(jobId)) {
                logger.info("创建TTS任务成功，jobId: {}, breakingSentenceId: {}", jobId, breakingSentenceId);
                
                // 在事务中保存job_id和更新状态
                updateJobIdAndStatusInTransaction(breakingSentenceId, jobId, SynthesisStatus.Status.PROCESSING);
            } else {
                logger.warn("创建TTS任务失败：未返回jobId，breakingSentenceId: {}", breakingSentenceId);
                // 在事务中更新状态为失败
                updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
            }
            
        } catch (ConnectionException e) {
            logger.error("创建TTS任务连接异常，breakingSentenceId: {}", breakingSentenceId, e);
            updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
        } catch (RequestTimeoutException e) {
            logger.error("创建TTS任务请求超时，breakingSentenceId: {}", breakingSentenceId, e);
            updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
        } catch (ServiceResponseException e) {
            logger.error("创建TTS任务服务响应异常，breakingSentenceId: {}, HTTP状态码={}, 错误码={}, 错误信息={}", 
                    breakingSentenceId, e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
        } catch (Exception e) {
            logger.error("创建TTS任务异常，breakingSentenceId: {}", breakingSentenceId, e);
            updateSynthesisStatusInTransaction(breakingSentenceId, SynthesisStatus.Status.FAILED, null);
        }
    }
    
    /**
     * 在事务中更新jobId和状态
     */
    private void updateJobIdAndStatusInTransaction(Long breakingSentenceId, String jobId, Integer status) {
        try {
            transactionTemplate.execute(transactionStatus -> {
                // 保存job_id到数据库
                breakingSentenceMapper.updateJobId(breakingSentenceId, jobId);
                // 更新状态为合成中
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, status, null, null);
                logger.debug("在事务中更新jobId和状态成功，breakingSentenceId: {}, jobId: {}, status: {}", 
                        breakingSentenceId, jobId, status);
                return null;
            });
            logger.info("事务提交成功，已更新jobId和状态，breakingSentenceId: {}, jobId: {}, status: {}", 
                    breakingSentenceId, jobId, status);
        } catch (Exception e) {
            logger.error("在事务中更新jobId和状态失败，breakingSentenceId: {}, jobId: {}, status: {}", 
                    breakingSentenceId, jobId, status, e);
            throw e;
        }
    }
    
    /**
     * 在事务中更新合成状态
     */
    private void updateSynthesisStatusInTransaction(Long breakingSentenceId, Integer status, String jobId) {
        try {
            transactionTemplate.execute(transactionStatus -> {
                breakingSentenceMapper.updateSynthesisInfo(breakingSentenceId, status, null, null);
                if (jobId != null) {
                    breakingSentenceMapper.updateJobId(breakingSentenceId, jobId);
                }
                logger.debug("在事务中更新状态成功，breakingSentenceId: {}, status: {}", 
                        breakingSentenceId, status);
                return null;
            });
            logger.info("事务提交成功，已更新状态，breakingSentenceId: {}, status: {}", 
                    breakingSentenceId, status);
        } catch (Exception e) {
            logger.error("在事务中更新状态失败，breakingSentenceId: {}, status: {}", 
                    breakingSentenceId, status, e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    @PreDestroy
    public void stopConsuming() {
        running = false;
        rateLimiterExecutor.shutdown();
        if (consumer != null) {
            try {
                consumer.close();
                logger.info("TTS合成请求消费者停止");
            } catch (Exception e) {
                logger.error("关闭消费者失败", e);
            }
        }
    }
}

