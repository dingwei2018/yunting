package com.yunting.config;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 * 配置 Producer 和 Consumer Bean
 */
@Configuration
public class RocketMQConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQConfig.class);
    
    @Value("${rocketmq.proxy-endpoint}")
    private String proxyEndpoint;
    
    @Value("${rocketmq.tts.topic}")
    private String ttsTopic;
    
    @Value("${rocketmq.tts-callback.consumer-group}")
    private String ttsCallbackConsumerGroup;
    
    @Value("${rocketmq.tts-callback.consumption-thread-count:5}")
    private int consumptionThreadCount;
    
    @Value("${rocketmq.tts-synthesis.consumer-group}")
    private String ttsSynthesisConsumerGroup;
    
    @Value("${rocketmq.audio-merge.consumer-group}")
    private String audioMergeConsumerGroup;
    
    @Value("${rocketmq.audio-merge.consumption-thread-count:3}")
    private int audioMergeConsumptionThreadCount;
    
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    /**
     * 创建 RocketMQ Producer Bean（共用）
     * 用于发送 TTS_CALLBACK 和 TTS_SYNTHESIS 两种类型的消息
     * 注意：如果使用顺序消息，需要确保 Topic 支持 FIFO 消息类型
     */
    @Bean(destroyMethod = "close")
    public Producer ttsProducer() {
        try {
            ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                    .setEndpoints(proxyEndpoint);
            ClientConfiguration configuration = builder.build();
            
            Producer producer = provider.newProducerBuilder()
                    .setTopics(ttsTopic)
                    .setClientConfiguration(configuration)
                    .build();
            
            logger.info("RocketMQ Producer 初始化成功，Topic: {}", ttsTopic);
            return producer;
        } catch (Exception e) {
            logger.error("RocketMQ Producer 初始化失败", e);
            throw new RuntimeException("RocketMQ Producer 初始化失败", e);
        }
    }
    
    /**
     * 获取 RocketMQ Proxy 端点配置
     */
    public String getProxyEndpoint() {
        return proxyEndpoint;
    }
    
    /**
     * 获取 TTS Topic（共用）
     */
    public String getTtsTopic() {
        return ttsTopic;
    }
    
    /**
     * 获取 TTS 回调 Consumer Group
     */
    public String getTtsCallbackConsumerGroup() {
        return ttsCallbackConsumerGroup;
    }
    
    /**
     * 获取 TTS 合成请求 Consumer Group
     */
    public String getTtsSynthesisConsumerGroup() {
        return ttsSynthesisConsumerGroup;
    }
    
    /**
     * 获取消费线程数
     */
    public int getConsumptionThreadCount() {
        return consumptionThreadCount;
    }
    
    /**
     * 获取音频合并 Consumer Group
     */
    public String getAudioMergeConsumerGroup() {
        return audioMergeConsumerGroup;
    }
    
    /**
     * 获取音频合并消费线程数
     */
    public int getAudioMergeConsumptionThreadCount() {
        return audioMergeConsumptionThreadCount;
    }
    
    /**
     * 获取 ClientServiceProvider
     */
    public ClientServiceProvider getProvider() {
        return provider;
    }
}
