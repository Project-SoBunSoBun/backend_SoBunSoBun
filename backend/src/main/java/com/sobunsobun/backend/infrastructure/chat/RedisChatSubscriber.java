package com.sobunsobun.backend.infrastructure.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber {

    private static final String DESTINATION_PREFIX = "/topic/rooms/";

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(String message) {
        try {
            ChatMessageResponse response = objectMapper.readValue(message, ChatMessageResponse.class);
            messagingTemplate.convertAndSend(DESTINATION_PREFIX + response.getRoomId(), response);
        } catch (Exception ex) {
            log.error("Redis 채팅 메시지 수신 처리 중 오류가 발생했습니다. payload={}", message, ex);
        }
    }
}

