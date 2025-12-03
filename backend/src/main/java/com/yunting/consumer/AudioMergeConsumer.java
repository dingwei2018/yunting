package com.yunting.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.config.RocketMQConfig;
import com.yunting.dto.audio.AudioMergeMessage;
import com.yunting.service.AudioMergeService;
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
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * 音频合并消息消费者
 * 从 RocketMQ 消费音频合并消息，并发处理数限制为3
 */
@Component
public class AudioMergeConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioMergeConsumer.class);
    
    private PushConsumer consumer;
    private final AudioMergeService audioMergeService;
    private final ObjectMapper objectMapper;
    private final RocketMQConfig rocketMQConfig;
    
    public AudioMergeConsumer(AudioMergeService audioMergeService,
                              ObjectMapper objectMapper,
                              RocketMQConfig rocketMQConfig) {
        this.audioMergeService = audioMergeService;
        this.objectMapper = objectMapper;
        this.rocketMQConfig = rocketMQConfig;
    }
    
    @PostConstruct
    public void startConsuming() {
        logger.info("音频合并消息消费者启动");
        
        // 使用后台线程延迟启动，避免 Topic 不存在时应用启动失败
        new Thread(() -> {
            int maxRetries = 10;
            int retryInterval = 5000; // 5秒重试一次
            
            for (int i = 0; i < maxRetries; i++) {
                try {
                    // 创建客户端配置
                    ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                            .setEndpoints(rocketMQConfig.getProxyEndpoint());
                    ClientConfiguration configuration = builder.build();
                    
                    // 订阅Topic，使用Tag过滤
                    FilterExpression filterExpression = new FilterExpression("AUDIO_MERGE", FilterExpressionType.TAG);
                    
                    // 创建消息监听器
                    MessageListener messageListener = new MessageListener() {
                        @Override
                        public ConsumeResult consume(MessageView messageView) {
                            return processMessage(messageView);
                        }
                    };
                    
                    // 创建 PushConsumer
                    consumer = rocketMQConfig.getProvider().newPushConsumerBuilder()
                            .setClientConfiguration(configuration)
                            .setConsumerGroup(rocketMQConfig.getAudioMergeConsumerGroup())
                            .setSubscriptionExpressions(Collections.singletonMap(
                                    rocketMQConfig.getTtsTopic(), filterExpression))  // 使用共用的 Topic
                            .setConsumptionThreadCount(rocketMQConfig.getAudioMergeConsumptionThreadCount())  // 设置并发线程数
                            .setMessageListener(messageListener)
                            .build();
                    
                    logger.info("音频合并消息监听器注册成功，Topic: {}, ConsumerGroup: {}, 并发线程数: {}", 
                            rocketMQConfig.getTtsTopic(), 
                            rocketMQConfig.getAudioMergeConsumerGroup(),
                            rocketMQConfig.getAudioMergeConsumptionThreadCount());
                    return; // 成功启动，退出循环
                    
                } catch (Exception e) {
                    if (i < maxRetries - 1) {
                        logger.warn("注册音频合并消息监听器失败（尝试 {}/{}），将在 {} 秒后重试。错误: {}", 
                                i + 1, maxRetries, retryInterval / 1000, e.getMessage());
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.error("重试等待被中断", ie);
                            return;
                        }
                    } else {
                        logger.error("注册音频合并消息监听器失败，已达到最大重试次数（{}），Consumer 将无法启动", maxRetries, e);
                    }
                }
            }
        }, "AudioMergeConsumer-Startup-Thread").start();
    }
    
    /**
     * 处理单条消息
     * RocketMQ 会根据配置的并发线程数自动控制并发
     */
    private ConsumeResult processMessage(MessageView messageView) {
        String messageId = messageView.getMessageId().toString();
        try {
            // 解析消息体
            ByteBuffer byteBuffer = messageView.getBody();
            byte[] body = new byte[byteBuffer.remaining()];
            byteBuffer.get(body);
            AudioMergeMessage mergeMessage = objectMapper.readValue(body, AudioMergeMessage.class);
            
            logger.info("收到音频合并消息，taskId: {}, mergeId: {}, messageId: {}, 线程: {}", 
                    mergeMessage.getTaskId(), mergeMessage.getMergeId(), messageId, Thread.currentThread().getName());
            
            // 调用业务逻辑处理合并
            audioMergeService.processAudioMerge(mergeMessage);
            
            logger.info("音频合并消息处理成功，taskId: {}, mergeId: {}, messageId: {}", 
                    mergeMessage.getTaskId(), mergeMessage.getMergeId(), messageId);
            
            // 处理成功，返回SUCCESS，消息会被确认
            return ConsumeResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("处理音频合并消息失败，messageId: {}", messageId, e);
            // 处理失败，返回FAILURE，RocketMQ会自动重试
            return ConsumeResult.FAILURE;
        }
    }
    
    @PreDestroy
    public void stopConsuming() {
        if (consumer != null) {
            try {
                consumer.close();
                logger.info("音频合并消息消费者停止");
            } catch (Exception e) {
                logger.error("关闭音频合并消费者失败", e);
            }
        }
    }
}

