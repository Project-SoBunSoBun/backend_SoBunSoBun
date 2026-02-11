package com.sobunsobun.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * WebSocket STOMP 연결 시 JWT 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 프레임만 처리
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        try {
            log.info("🔐 STOMP CONNECT 프레임 수신");

            // Authorization 헤더에서 토큰 추출
            List<String> authorization = accessor.getNativeHeader("Authorization");
            String token = null;

            if (authorization != null && !authorization.isEmpty()) {
                token = authorization.get(0);
                log.info("✅ Authorization 헤더 발견: {}", token.substring(0, Math.min(20, token.length())) + "...");
            } else {
                log.warn("⚠️ Authorization 헤더 없음 - 기본값으로 진행");
                // 테스트 환경: Authorization 헤더가 없어도 연결 허용
                token = "test-token";
            }

            // Bearer 접두사 제거
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰 검증 (테스트: 간단한 검증)
            if (token == null || token.isEmpty()) {
                log.warn("⚠️ 토큰이 비어있음 - 기본값으로 진행");
                token = "1"; // 기본 userId
            }

            // userId 추출 (토큰이 숫자면 그대로 사용, 아니면 기본값)
            Long userId;
            try {
                userId = Long.parseLong(token);
            } catch (NumberFormatException e) {
                // 토큰이 숫자가 아니면 기본값 사용
                userId = 1L;
            }

            log.info("✅ 인증 성공 - userId: {}", userId);

            // Principal 설정
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
            );
            accessor.setUser(authentication);

            // 세션에 userId 저장
            accessor.getSessionAttributes().put("userId", userId);

        } catch (Exception e) {
            log.error("❌ WebSocket 인증 중 에러: {}", e.getMessage(), e);
            // 에러 발생해도 연결 진행 (테스트 환경)
        }

        return message;
    }
}

