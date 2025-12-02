package com.yunting.config;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * RocketMQ 配置类
 * 配置 Producer 和 Consumer Bean
 */
@Configuration
public class RocketMQConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQConfig.class);
    
    @Value("${rocketmq.proxy-endpoint}")
    private String proxyEndpoint;
    
    @Value("${rocketmq.tts-callback.topic}")
    private String ttsCallbackTopic;
    
    @Value("${rocketmq.tts-callback.consumer-group}")
    private String ttsCallbackConsumerGroup;
    
    @Value("${rocketmq.tts-callback.consumption-thread-count:5}")
    private int consumptionThreadCount;
    
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    /**
     * 创建 RocketMQ Producer Bean
     */
    @Bean(destroyMethod = "close")
    public Producer ttsCallbackProducer() {
        try {
            ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                    .setEndpoints(proxyEndpoint);
            ClientConfiguration configuration = builder.build();
            
            Producer producer = provider.newProducerBuilder()
                    .setTopics(ttsCallbackTopic)
                    .setClientConfiguration(configuration)
                    .build();
            
            logger.info("RocketMQ Producer 初始化成功，Topic: {}", ttsCallbackTopic);
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
     * 获取 TTS 回调 Topic
     */
    public String getTtsCallbackTopic() {
        return ttsCallbackTopic;
    }
    
    /**
     * 获取 TTS 回调 Consumer Group
     */
    public String getTtsCallbackConsumerGroup() {
        return ttsCallbackConsumerGroup;
    }
    
    /**
     * 获取消费线程数
     */
    public int getConsumptionThreadCount() {
        return consumptionThreadCount;
    }
    
    /**
     * 获取 ClientServiceProvider
     */
    public ClientServiceProvider getProvider() {
        return provider;
    }
}
