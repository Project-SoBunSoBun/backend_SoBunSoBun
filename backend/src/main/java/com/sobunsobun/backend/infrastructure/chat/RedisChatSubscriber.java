package com.sobunsobun.backend.infrastructure.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            log.debug("Redis 메시지 수신: {}", json);

            ChatMessageResponse payload =
                    objectMapper.readValue(json, ChatMessageResponse.class);

            // 이후 로직 처리…

        } catch (Exception e) {
            log.error("Redis 채팅 메시지 파싱 오류: {}",
                    new String(message.getBody(), StandardCharsets.UTF_8), e);
        }
    }
}