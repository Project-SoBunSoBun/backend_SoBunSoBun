package com.sobunsobun.backend.config;

import com.sobunsobun.backend.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정
 *
 * 라우팅:
 * - /ws/chat : 클라이언트 연결 엔드포인트
 * - /app/chat/* : 클라이언트에서 서버로 메시지 전송
 * - /topic/rooms/{roomId} : 채팅방 메시지 브로드캐스팅
 * - /user/{userId}/queue/* : 개인 알림
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * TaskScheduler 빈 - heartbeat 및 스케줄링 작업용
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-scheduler-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");  // 모든 origin 허용

        // SockJS 제거 - WebSocket만 사용하여 400 오류 해결
        // SockJS는 HTTP 핸드셰이크에서 400 BAD_REQUEST 발생
        // 최신 브라우저는 모두 WebSocket 지원하므로 SockJS 불필요

        log.info("✅ WebSocket STOMP endpoint registered: /ws-stomp");
        log.info("📡 WebSocket only (SockJS fallback disabled)");
        log.info("🔓 CORS: All origins allowed");
        log.info("💓 Heartbeat: 25 seconds");
        log.info("🔐 Authentication: JWT at STOMP CONNECT frame");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 내장 메시지 브로커 설정
        // /sub: 채팅 목록 실시간 업데이트 개인 채널 (/sub/users/{userId}/chat-list)
        config.enableSimpleBroker("/topic", "/queue", "/sub")
                .setTaskScheduler(taskScheduler())
                .setHeartbeatValue(new long[]{30000, 30000});

        // 클라이언트 → 서버: /app/** 형식
        config.setApplicationDestinationPrefixes("/app");

        // 개인 메시지 destination prefix
        config.setUserDestinationPrefix("/user");

        log.info("✅ Message broker configured");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // STOMP CONNECT 프레임에서 JWT 검증
        registration.interceptors(webSocketAuthInterceptor);
        log.info("✅ WebSocket JWT Auth Interceptor registered");
    }
}


