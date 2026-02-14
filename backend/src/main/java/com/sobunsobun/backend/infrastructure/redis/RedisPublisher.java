package com.sobunsobun.backend.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Redis Pub/Sub Publisher
 *
 * 클라이언트로부터 수신한 메시지를 Redis의 특정 Topic(채널)으로 발행합니다.
 *
 * Pub/Sub 흐름:
 * 1. 클라이언트 → ChatController → ChatService
 * 2. ChatService → RedisPublisher (메시지 발행)
 * 3. Redis Topic에 메시지 발행
 * 4. RedisSubscriber가 감지하고 처리
 * 5. SimpMessagingTemplate으로 WebSocket 구독자에게 브로드캐스트
 */
@Slf4j
@Component
public class RedisPublisher {

    private final Optional<RedisTemplate<String, String>> redisTemplate;
    private final ObjectMapper objectMapper;
    private boolean redisAvailable = false;

    @Autowired
    public RedisPublisher(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.objectMapper = objectMapper;
        this.redisAvailable = this.redisTemplate.isPresent();

        if (!redisAvailable) {
            log.warn("⚠️ RedisPublisher - Redis가 사용 불가능합니다. 실시간 채팅이 제한됩니다.");
        }
    }

    /**
     * 채팅 메시지를 Redis Topic으로 발행
     *
     * @param roomId 채팅방 ID
     * @param chatMessageDto 발행할 메시지 DTO
     */
    public void publish(Long roomId, ChatMessageDto chatMessageDto) {
        if (!redisAvailable) {
            log.warn("⚠️ Redis 미사용: 메시지 발행 건너뜀 - roomId: {}", roomId);
            return;
        }

        try {
            // 채팅방별 Topic 이름: chat:room:{roomId}
            String topic = "chat:room:" + roomId;

            // DTO를 JSON 문자열로 직렬화
            String jsonMessage = objectMapper.writeValueAsString(chatMessageDto);

            // Redis Topic으로 메시지 발행
            redisTemplate.get().convertAndSend(topic, jsonMessage);

            log.info("[Redis Publish] Topic: {}, Message: {}", topic, jsonMessage);
        } catch (Exception e) {
            log.error("[Redis Publish Error] roomId: {}, error: {}", roomId, e.getMessage(), e);
            throw new RuntimeException("Redis 메시지 발행 실패", e);
        }
    }
}
