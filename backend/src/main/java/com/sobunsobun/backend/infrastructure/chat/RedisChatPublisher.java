package com.sobunsobun.backend.infrastructure.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatPublisher {

    private static final String CHANNEL_PREFIX = "chat.room.";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long roomId, ChatMessageResponse response) {
        try {
            // (1) JSON 직렬화
            String payload = objectMapper.writeValueAsString(response);

            // (2) Redis pub
            stringRedisTemplate.convertAndSend(buildChannel(roomId), payload);

            log.debug("Redis Publish 완료 → channel={}, payload={}", buildChannel(roomId), payload);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("채팅 메시지 직렬화(JSON 변환)에 실패했습니다.", e);
        }
    }

    private String buildChannel(Long roomId) {
        return CHANNEL_PREFIX + roomId;
    }
}