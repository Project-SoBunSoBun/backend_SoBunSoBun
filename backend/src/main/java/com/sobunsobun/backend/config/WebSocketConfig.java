package com.sobunsobun.backend.config;

import com.sobunsobun.backend.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket(STOMP) 설정
 *
 * STOMP 라우팅:
 * - /app/chat/** : 클라이언트에서 서버로 메시지 전송 (ChannelInterceptor로 권한 검증)
 * - /topic/rooms/{roomId} : 채팅방 구독 (브로드캐스팅)
 * - /user/{userId}/queue/private : 개인 메시지 큐 (읽음 처리, 개인 알림)
 * - /topic/rooms/{roomId}/events : 채팅방 이벤트 (멤버 변경, 방 삭제 등)
 *
 * 보안:
 * - Handshake 시 JWT 토큰 검증 (WebSocketAuthInterceptor)
 * - CONNECT 프레임에서 토큰 추출 및 사용자 인증
 * - Subscribe/Send 시 권한 검증 (room member 확인)
 *
 * WebSocket vs REST:
 * - WebSocket: 실시간 메시지, 읽음 처리, 이벤트
 * - REST: 메시지 목록 조회, 이미지 업로드, 초대 관리
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * WebSocket Endpoint 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws/chat")
                // 모든 origin 허용 (와일드카드는 정확한 origin으로 변환)
                .setAllowedOriginPatterns("*");
                // SockJS 제거 - 순수 WebSocket만 사용

        log.info("✅ WebSocket endpoint registered: /ws/chat (pure WebSocket)");
        log.info("📡 CORS: All origin patterns allowed for WebSocket");
    }

    /**
     * 메시지 브로커 설정
     *
     * applicationDestinationPrefixes: "/app"
     * - 클라이언트에서 /app/... 로 시작하는 메시지는 @MessageMapping이 처리
     *
     * brokerRegistry:
     * - /topic : 공개 채널 (브로드캐스팅)
     * - /queue : 개인 메시지 큐 (convertAndSendToUser 사용)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 내장 브로커: /topic (브로드캐스트), /queue (개인 메시지)
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 /user/{userId}/queue/... 로 개인 메시지 받기
        config.setUserDestinationPrefix("/user");

        // 클라이언트 → 서버 메시지 접두사 (/app/... 로 시작하는 메시지를 @MessageMapping으로 라우팅)
        config.setApplicationDestinationPrefixes("/app");

        log.info("Message broker configured with /app, /topic, /queue prefixes");
    }

    /**
     * 애플리케이션 메시지 처리 설정
     *
     * /app으로 시작하는 메시지는 @MessageMapping으로 처리
     */
    public void configureApplicationContext(org.springframework.web.servlet.config.annotation.WebMvcConfigurer configurer) {
        // Spring Boot 3에서는 @MessageMapping이 자동으로 /app 접두사 처리
    }

    /**
     * 채널 인터셉터 등록 (WebSocket 보안)
     *
     * 클라이언트의 모든 STOMP 프레임(CONNECT, SUBSCRIBE, SEND, DISCONNECT)을
     * 사전 검증하고 사용자 정보를 세션에 저장
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
        log.info("✅ WebSocketAuthInterceptor registered");
        log.info("📍 Application destination prefixes: /app");
        log.info("📍 Simple broker destinations: /topic, /queue");
    }
}
