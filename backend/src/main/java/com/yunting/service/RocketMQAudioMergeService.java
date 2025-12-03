package com.yunting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.dto.audio.AudioMergeMessage;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RocketMQ 音频合并消息发送服务
 * 负责将音频合并请求发送到 RocketMQ 消息队列
 */
@Service
public class RocketMQAudioMergeService {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQAudioMergeService.class);
    
    private final Producer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    public RocketMQAudioMergeService(Producer ttsProducer,
                                     ObjectMapper objectMapper,
                                     @Value("${rocketmq.tts.topic}") String topic) {
        this.producer = ttsProducer;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }
    
    /**
     * 发送音频合并消息到 RocketMQ
     * 
     * @param mergeMessage 音频合并消息
     * @return 是否发送成功
     */
    public boolean sendAudioMergeMessage(AudioMergeMessage mergeMessage) {
        try {
            // 序列化消息为JSON
            byte[] messageBody = objectMapper.writeValueAsBytes(mergeMessage);
            
            // 构建消息
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(String.valueOf(mergeMessage.getMergeId()))  // 使用mergeId作为Key，便于消息追踪
                    .setTag("AUDIO_MERGE")
                    .setBody(messageBody)
                    .build();
            
            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            logger.info("音频合并消息发送成功，taskId: {}, mergeId: {}, messageId: {}", 
                    mergeMessage.getTaskId(), mergeMessage.getMergeId(), sendReceipt.getMessageId());
            return true;
        } catch (Exception e) {
            logger.error("音频合并消息发送失败，taskId: {}, mergeId: {}", 
                    mergeMessage.getTaskId(), mergeMessage.getMergeId(), e);
            return false;
        }
    }
}

