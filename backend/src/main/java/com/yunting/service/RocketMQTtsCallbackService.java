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
 * 使用顺序消息，确保所有回调消息按全局顺序执行
 */
@Service
public class RocketMQTtsCallbackService {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQTtsCallbackService.class);
    
    private final Producer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String messageGroup;
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    public RocketMQTtsCallbackService(Producer ttsProducer,
                                      ObjectMapper objectMapper,
                                      @Value("${rocketmq.tts.topic}") String topic,
                                      @Value("${rocketmq.tts-callback.message-group:TTS_CALLBACK_ORDERED}") String messageGroup) {
        this.producer = ttsProducer;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.messageGroup = messageGroup;
    }
    
    /**
     * 发送TTS回调消息到RocketMQ（顺序消息）
     * 
     * @param callbackRequest TTS回调请求
     * @return 是否发送成功
     */
    public boolean sendTtsCallbackMessage(TtsCallbackRequest callbackRequest) {
        try {
            // 序列化回调请求为JSON
            byte[] messageBody = objectMapper.writeValueAsBytes(callbackRequest);
            
            // 构建消息（顺序消息）
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(callbackRequest.getJobId())  // 使用jobId作为Key，便于消息追踪
                    .setTag("TTS_CALLBACK")
                    .setMessageGroup(messageGroup)  // 设置消息组，确保所有消息按顺序执行
                    .setBody(messageBody)
                    .build();
            
            // 发送顺序消息
            SendReceipt sendReceipt = producer.send(message);
            logger.info("TTS回调消息发送成功（顺序消息），jobId: {}, messageId: {}, messageGroup: {}", 
                    callbackRequest.getJobId(), sendReceipt.getMessageId(), messageGroup);
            return true;
        } catch (IllegalArgumentException e) {
            // 处理 Topic 不支持 FIFO 消息类型的错误
            if (e.getMessage() != null && e.getMessage().contains("not match with topic accept message types")) {
                logger.error("TTS回调消息发送失败：Topic {} 不支持 FIFO 消息类型。请确保 Topic 配置为支持 FIFO 消息类型，或使用支持 FIFO 的 Topic。jobId: {}", 
                        topic, callbackRequest.getJobId());
                logger.error("解决方案：1) 在 RocketMQ 控制台将 Topic '{}' 配置为支持 FIFO 消息类型；2) 或创建新的支持 FIFO 的 Topic，并配置 rocketmq.tts.topic", topic);
            } else {
                logger.error("TTS回调消息发送失败，jobId: {}", 
                        callbackRequest.getJobId(), e);
            }
            return false;
        } catch (Exception e) {
            logger.error("TTS回调消息发送失败，jobId: {}", callbackRequest.getJobId(), e);
            return false;
        }
    }
}

