package com.sobunsobun.backend.infrastructure.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ChatMessageResponse response = objectMapper.readValue(payload, ChatMessageResponse.class);
            Long roomId = response.getRoomId(); // 혹은 getRoomId()

            // STOMP 구독자들에게 브로드캐스트
            String destination = "/sub/chat/rooms/" + roomId;
            messagingTemplate.convertAndSend(destination, response);

        } catch (Exception e) {
            log.error("Redis 채팅 메시지 수신 처리 중 오류가 발생했습니다. payload={}", payload, e);
        }
    }
}