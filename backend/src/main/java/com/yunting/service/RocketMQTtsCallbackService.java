package com.yunting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.dto.synthesis.TtsCallbackRequest;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RocketMQ TTS回调消息发送服务
 * 负责将TTS回调请求发送到RocketMQ消息队列
 */
@Service
public class RocketMQTtsCallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQTtsCallbackService.class);
    
    private final Producer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    public RocketMQTtsCallbackService(Producer ttsCallbackProducer,
                                      ObjectMapper objectMapper,
                                      @Value("${rocketmq.tts-callback.topic}") String topic) {
        this.producer = ttsCallbackProducer;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }
    
    /**
     * 发送TTS回调消息到RocketMQ
     * 
     * @param callbackRequest TTS回调请求
     * @return 是否发送成功
     */
    public boolean sendTtsCallbackMessage(TtsCallbackRequest callbackRequest) {
        try {
            // 序列化回调请求为JSON
            byte[] messageBody = objectMapper.writeValueAsBytes(callbackRequest);
            
            // 构建消息
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(callbackRequest.getJobId())  // 使用jobId作为Key，便于消息追踪
                    .setTag("TTS_CALLBACK")
                    .setBody(messageBody)
                    .build();
            
            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            logger.info("TTS回调消息发送成功，jobId: {}, messageId: {}", 
                    callbackRequest.getJobId(), sendReceipt.getMessageId());
            return true;
        } catch (Exception e) {
            logger.error("TTS回调消息发送失败，jobId: {}", callbackRequest.getJobId(), e);
            return false;
        }
    }
}

