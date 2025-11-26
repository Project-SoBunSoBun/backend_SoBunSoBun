package com.sobunsobun.backend.infrastructure.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisChatPublisher {

    private static final String CHANNEL_PREFIX = "chat.room.";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatPublisher(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Long roomId, ChatMessageResponse response) {
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend(buildChannel(roomId), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 메시지 직렬화에 실패했습니다.", e);
        }
    }

    public String buildChannel(Long roomId) {
        return CHANNEL_PREFIX + roomId;
    }
}

