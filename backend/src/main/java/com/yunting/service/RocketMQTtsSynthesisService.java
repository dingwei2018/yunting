package com.yunting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunting.dto.synthesis.TtsSynthesisRequest;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RocketMQ TTS合成请求发送服务
 * 负责将TTS合成请求发送到RocketMQ消息队列
 */
@Service
public class RocketMQTtsSynthesisService {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQTtsSynthesisService.class);
    
    private final Producer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    public RocketMQTtsSynthesisService(Producer ttsProducer,
                                      ObjectMapper objectMapper,
                                      @Value("${rocketmq.tts.topic}") String topic) {
        this.producer = ttsProducer;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }
    
    /**
     * 发送TTS合成请求到RocketMQ
     * 
     * @param request TTS合成请求
     * @return 是否发送成功
     */
    public boolean sendSynthesisRequest(TtsSynthesisRequest request) {
        try {
            // 序列化请求为JSON
            byte[] messageBody = objectMapper.writeValueAsBytes(request);
            
            // 构建消息
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(String.valueOf(request.getBreakingSentenceId()))  // 使用breakingSentenceId作为Key
                    .setTag("TTS_SYNTHESIS")  // 使用 TTS_SYNTHESIS Tag
                    .setBody(messageBody)
                    .build();
            
            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            logger.info("TTS合成请求发送成功，breakingSentenceId: {}, messageId: {}", 
                    request.getBreakingSentenceId(), sendReceipt.getMessageId());
            return true;
        } catch (Exception e) {
            logger.error("TTS合成请求发送失败，breakingSentenceId: {}", 
                    request.getBreakingSentenceId(), e);
            return false;
        }
    }
}

