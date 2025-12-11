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
 * 使用顺序消息，确保所有合成请求按顺序执行
 */
@Service
public class RocketMQTtsSynthesisService {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQTtsSynthesisService.class);
    
    private final Producer producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String messageGroup;
    private final ClientServiceProvider provider = ClientServiceProvider.loadService();
    
    public RocketMQTtsSynthesisService(Producer ttsProducer,
                                      ObjectMapper objectMapper,
                                      @Value("${rocketmq.tts.topic}") String topic,
                                      @Value("${rocketmq.tts-synthesis.message-group:TTS_SYNTHESIS_ORDERED}") String messageGroup) {
        this.producer = ttsProducer;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.messageGroup = messageGroup;
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
            
            // 构建消息（顺序消息）
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys(String.valueOf(request.getBreakingSentenceId()))  // 使用breakingSentenceId作为Key
                    .setTag("TTS_SYNTHESIS")  // 使用 TTS_SYNTHESIS Tag
                    .setMessageGroup(messageGroup)  // 设置消息组，确保所有消息按顺序执行
                    .setBody(messageBody)
                    .build();
            
            // 发送顺序消息
            SendReceipt sendReceipt = producer.send(message);
            logger.info("TTS合成请求发送成功（顺序消息），breakingSentenceId: {}, messageId: {}, messageGroup: {}", 
                    request.getBreakingSentenceId(), sendReceipt.getMessageId(), messageGroup);
            return true;
        } catch (IllegalArgumentException e) {
            // 处理 Topic 不支持 FIFO 消息类型的错误
            if (e.getMessage() != null && e.getMessage().contains("not match with topic accept message types")) {
                logger.error("TTS合成请求发送失败：Topic {} 不支持 FIFO 消息类型。请确保 Topic 配置为支持 FIFO 消息类型，或使用支持 FIFO 的 Topic。breakingSentenceId: {}", 
                        topic, request.getBreakingSentenceId());
                logger.error("解决方案：1) 在 RocketMQ 控制台将 Topic '{}' 配置为支持 FIFO 消息类型；2) 或创建新的支持 FIFO 的 Topic，并配置 rocketmq.tts-synthesis.topic", topic);
            } else {
                logger.error("TTS合成请求发送失败，breakingSentenceId: {}", 
                        request.getBreakingSentenceId(), e);
            }
            return false;
        } catch (Exception e) {
            logger.error("TTS合成请求发送失败，breakingSentenceId: {}", 
                    request.getBreakingSentenceId(), e);
            return false;
        }
    }
}

