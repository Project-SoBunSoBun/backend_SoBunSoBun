package com.sobunsobun.backend.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.dto.chat.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub Subscriber
 *
 * Redis Topic에 발행된 메시지를 감지하고,
 * 수신한 메시지를 역직렬화(JSON → Object)한 뒤,
 * SimpMessagingTemplate을 이용해 WebSocket 구독자들에게 전송합니다.
 *
 * WebSocket 브로드캐스트 대상: /sub/chat/room/{roomId}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Topic에서 메시지 수신 및 처리
     *
     * 처리 흐름:
     * 1. Redis에서 메시지 수신
     * 2. JSON 문자열을 ChatMessageDto로 역직렬화
     * 3. SimpMessagingTemplate으로 WebSocket 브로드캐스트
     *
     * @param message Redis 메시지
     * @param pattern Redis Topic 패턴 (예: chat:room:1)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Redis 메시지를 String으로 변환
            String messageBody = new String(message.getBody(), "UTF-8");
            String topic = new String(pattern, "UTF-8");

            log.info("═══════════════════════════════════════════════════════════");
            log.info(" [Redis Subscribe] Topic: {}", topic);
            log.info(" [Raw Message Body] {}", messageBody);

            // JSON 문자열을 ChatMessageDto로 역직렬화
            ChatMessageDto chatMessage = objectMapper.readValue(
                    messageBody,
                    ChatMessageDto.class
            );
            log.info(" [파싱 완료] ChatMessageDto 변환됨");
            log.info("   - type: {}, roomId: {}, senderId: {}, message: {}",
                    chatMessage.getType(), chatMessage.getRoomId(),
                    chatMessage.getSenderId(), chatMessage.getMessage());

            // 채팅방 ID를 Topic에서 추출
            Long roomId = chatMessage.getRoomId();
            String destination = "/topic/chat/room/" + roomId;  //  /topic으로 변경

            log.info(" [브로드캐스트 시작] destination: {}", destination);
            log.info(" [메시지 내용] {}", chatMessage);

            // WebSocket 구독자에게 브로드캐스트
            try {
                messagingTemplate.convertAndSend(destination, chatMessage);
                log.info(" [브로드캐스트 성공] 메시지 전송 완료");
                log.info("   - 대상: {}", destination);
                log.info("   - 내용: {}", chatMessage.getMessage());
            } catch (Exception broadcastException) {
                log.error(" [브로드캐스트 실패] {}", broadcastException.getMessage(), broadcastException);
            }

            log.info("═══════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error(" [Redis Subscribe Error] {}", e.getMessage(), e);
        }
    }
}
