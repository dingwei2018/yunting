package com.yunting.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.config.RocketMQConfig;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import com.yunting.service.SynthesisService;
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
 * TTS回调消息消费者
 * 从RocketMQ消费TTS回调消息，并发处理数限制为5
 */
@Component
public class TtsCallbackConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(TtsCallbackConsumer.class);
    
    private PushConsumer consumer;
    private final SynthesisService synthesisService;
    private final ObjectMapper objectMapper;
    private final RocketMQConfig rocketMQConfig;
    
    public TtsCallbackConsumer(SynthesisService synthesisService,
                              ObjectMapper objectMapper,
                              RocketMQConfig rocketMQConfig) {
        this.synthesisService = synthesisService;
        this.objectMapper = objectMapper;
        this.rocketMQConfig = rocketMQConfig;
    }
    
    @PostConstruct
    public void startConsuming() {
        logger.info("TTS回调消息消费者启动");
        
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
                    FilterExpression filterExpression = new FilterExpression("TTS_CALLBACK", FilterExpressionType.TAG);
                    
                    // 创建消息监听器
                    MessageListener messageListener = new MessageListener() {
                        @Override
                        public ConsumeResult consume(MessageView messageView) {
                            return processMessage(messageView);
                        }
                    };
                    
                    // 创建 PushConsumer，在 builder 中设置 MessageListener
                    consumer = rocketMQConfig.getProvider().newPushConsumerBuilder()
                            .setClientConfiguration(configuration)
                            .setConsumerGroup(rocketMQConfig.getTtsCallbackConsumerGroup())
                            .setSubscriptionExpressions(Collections.singletonMap(
                                    rocketMQConfig.getTtsTopic(), filterExpression))  // 使用共用的 Topic
                            .setConsumptionThreadCount(rocketMQConfig.getConsumptionThreadCount())  // 设置并发线程数为5
                            .setMessageListener(messageListener)  // 在 builder 中设置监听器
                            .build();
                    
                    logger.info("TTS回调消息监听器注册成功，Topic: {}, ConsumerGroup: {}, 并发线程数: {}", 
                            rocketMQConfig.getTtsTopic(), 
                            rocketMQConfig.getTtsCallbackConsumerGroup(),
                            rocketMQConfig.getConsumptionThreadCount());
                    return; // 成功启动，退出循环
                    
                } catch (Exception e) {
                    if (i < maxRetries - 1) {
                        logger.warn("注册消息监听器失败（尝试 {}/{}），将在 {} 秒后重试。错误: {}", 
                                i + 1, maxRetries, retryInterval / 1000, e.getMessage());
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logger.error("重试等待被中断", ie);
                            return;
                        }
                    } else {
                        logger.error("注册消息监听器失败，已达到最大重试次数（{}），Consumer 将无法启动", maxRetries, e);
                        // 不抛出异常，允许应用继续运行，但 Consumer 不会工作
                        // 当 Producer 发送第一条消息时，Topic 会被自动创建，然后可以手动重启 Consumer
                    }
                }
            }
        }, "TtsCallbackConsumer-Startup-Thread").start();
    }
    
    /**
     * 处理单条消息
     * RocketMQ 会根据配置的并发线程数自动控制并发，最多5个线程同时处理
     */
    private ConsumeResult processMessage(MessageView messageView) {
        String messageId = messageView.getMessageId().toString();
        try {
            // 解析消息体
            ByteBuffer byteBuffer = messageView.getBody();
            byte[] body = new byte[byteBuffer.remaining()];
            byteBuffer.get(body);
            TtsCallbackRequest callbackRequest = objectMapper.readValue(body, TtsCallbackRequest.class);
            
            logger.info("收到TTS回调消息，jobId: {}, messageId: {}, 线程: {}", 
                    callbackRequest.getJobId(), messageId, Thread.currentThread().getName());
            
            // 调用业务逻辑处理回调
            synthesisService.handleTtsCallback(callbackRequest);
            
            logger.info("TTS回调消息处理成功，jobId: {}, messageId: {}", 
                    callbackRequest.getJobId(), messageId);
            
            // 处理成功，返回SUCCESS，消息会被确认
            return ConsumeResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("处理TTS回调消息失败，messageId: {}", messageId, e);
            // 处理失败，返回FAILURE，RocketMQ会自动重试
            return ConsumeResult.FAILURE;
        }
    }
    
    @PreDestroy
    public void stopConsuming() {
        if (consumer != null) {
            try {
                consumer.close();
                logger.info("TTS回调消息消费者停止");
            } catch (Exception e) {
                logger.error("关闭消费者失败", e);
            }
        }
    }
}
