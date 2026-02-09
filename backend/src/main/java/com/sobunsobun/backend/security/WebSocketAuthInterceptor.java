package com.sobunsobun.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket STOMP 인증 인터셉터
 *
 * 역할:
 * - 클라이언트의 CONNECT 프레임에서 JWT 토큰 추출
 * - 토큰 검증 및 사용자 인증
 * - 세션에 Authentication 정보 저장
 * - 이후 Subscribe/Send 요청에서 Principal 접근 가능
 *
 * iOS 클라이언트 예:
 * 1. WebSocket 연결: ws://api.example.com/ws/chat
 * 2. CONNECT 프레임 헤더에 Authorization: Bearer {jwt_token} 추가
 * 3. 서버가 토큰 검증 후 인증 정보 저장
 * 4. 이후 메시지 발송 시 Principal.name으로 userId 접근 가능
 *
 * 예외 처리:
 * - 토큰 없음: CONNECT 차단
 * - 토큰 유효하지 않음: CONNECT 차단
 * - 토큰 만료: CONNECT 차단
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 인바운드 메시지 사전 처리 (CONNECT, SUBSCRIBE, SEND, DISCONNECT)
     *
     * @param message STOMP 메시지
     * @param channel 메시지 채널
     * @return 처리된 메시지 (또는 null로 차단)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String messageType = accessor.getMessageType() != null ? accessor.getMessageType().toString() : "UNKNOWN";

        log.info("🌐 [preSend] WebSocket 메시지 수신 - 타입: {}", messageType);

        try {
            // CONNECT 프레임 처리 (JWT 검증)
            if ("CONNECT".equals(messageType)) {
                log.info("🔌 CONNECT 프레임 수신 - JWT 검증 시작");
                return handleConnect(accessor, message);
            }

            // SUBSCRIBE 프레임 로깅
            if ("SUBSCRIBE".equals(messageType)) {
                log.info("📡 SUBSCRIBE 프레임 수신 - destination: {}", accessor.getDestination());
                return message;
            }

            // SEND 프레임 로깅 (가장 중요!)
            if ("SEND".equals(messageType)) {
                String destination = accessor.getDestination();
                Object user = accessor.getUser();
                log.info("📤 [preSend] SEND 프레임 수신!!!");
                log.info("   - destination: {}", destination);
                log.info("   - user: {}", user);
                log.info("   - contentType: {}", accessor.getContentType());
                log.info("   - payload size: {}", message.getPayload() != null ? message.getPayload().toString().length() : 0);

                if (destination != null && destination.startsWith("/app/")) {
                    log.info("   ✅ /app으로 시작하는 destination - Controller로 라우팅됨");
                } else {
                    log.warn("   ⚠️ /app으로 시작하지 않는 destination: {}", destination);
                }

                return message;
            }

            // 다른 프레임은 통과 (이미 인증된 세션)
            log.debug("🔄 기타 프레임 통과 - 타입: {}", messageType);
            return message;
        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 처리 중 오류 [{}]: {}", messageType, e.getMessage(), e);
            return null; // 메시지 차단
        }
    }

    /**
     * CONNECT 프레임 처리 및 JWT 검증
     *
     * 클라이언트가 보낸 Authorization 헤더에서 토큰 추출 후 검증
     * 유효한 경우 UsernamePasswordAuthenticationToken을 세션에 저장
     *
     * @param accessor STOMP 메시지 헤더
     * @param message 원본 메시지
     * @return 처리된 메시지 (또는 null로 차단)
     */
    private Message<?> handleConnect(SimpMessageHeaderAccessor accessor, Message<?> message) {
        log.info("🔑 handleConnect 메서드 시작");

        // STOMP CONNECT 프레임의 native header에서 Authorization 추출
        List<String> authorization = accessor.getNativeHeader("Authorization");
        log.info("📋 Authorization 헤더 목록 (STOMP): {}", authorization);

        if (authorization == null || authorization.isEmpty()) {
            log.warn("❌ WebSocket CONNECT: Authorization 헤더 없음");
            return null;
        }

        String authHeader = authorization.get(0);
        log.info("📋 Authorization 헤더값: {} (길이: {})",
                authHeader.substring(0, Math.min(50, authHeader.length())) + "...",
                authHeader.length());

        String token = extractToken(authHeader);

        if (token == null) {
            log.warn("❌ WebSocket CONNECT: 토큰 추출 또는 검증 실패 - authHeader 형식 오류");
            return null;
        }

        // JWT 토큰 검증
        try {
            log.info("🔐 JWT 토큰 검증 시작 - token 길이: {}", token.length());
            Jws<Claims> claimsJws = jwtTokenProvider.parse(token);
            Claims claims = claimsJws.getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            log.info("✅ JWT 토큰 검증 성공 - userId: {}, role: {}", userId, role);

            if (userId == null) {
                log.warn("❌ WebSocket CONNECT: 토큰에 userId 정보 없음");
                return null;
            }

            // 권한 정보 생성
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            // Authentication 객체 생성
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId,                    // principal (사용자 ID)
                    null,                      // credentials (검증 완료)
                    authorities                // authorities
            );

            // 세션에 Authentication 저장
            accessor.setUser(auth);

            // SEND 메시지에서도 사용 가능하도록 세션 속성에 userId 저장
            accessor.getSessionAttributes().put("userId", userId);
            log.info("💾 세션 속성에 userId 저장: {}", userId);

            log.info("✅ WebSocket CONNECT 인증 성공 - userId: {}, role: {}, authorities: {}",
                    userId, role, authorities);
            return message;

        } catch (JwtException e) {
            log.error("❌ WebSocket CONNECT: JWT 검증 실패 - {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("❌ WebSocket CONNECT: 예기치 않은 오류", e);
            return null;
        }
    }

    /**
     * Authorization 헤더에서 토큰 추출
     *
     * 두 가지 형식을 지원:
     * 1. Bearer 형식: "Bearer eyJhbGciOiJIUzI1NiIsInR5..." → "eyJhbGciOiJIUzI1NiIsInR5..."
     * 2. Raw 토큰: "eyJhbGciOiJIUzI1NiIsInR5..." → "eyJhbGciOiJIUzI1NiIsInR5..."
     *
     * @param authHeader Authorization 헤더 값
     * @return 토큰 (또는 null)
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            log.warn("⚠️ Authorization 헤더가 비어있음");
            return null;
        }

        authHeader = authHeader.trim();
        String token;

        // Bearer 형식이면 "Bearer " 제거, 아니면 raw 토큰으로 처리
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
            log.debug("✅ Bearer 형식의 Authorization 헤더 감지");
        } else {
            token = authHeader;
            log.debug("✅ Raw 토큰 형식의 Authorization 헤더 감지 (Bearer 접두사 없음)");
        }

        if (token.isEmpty()) {
            log.warn("⚠️ 토큰이 비어있음");
            return null;
        }

        // JWT 토큰 기본 형식 검증 (3개의 .으로 구분)
        int dotCount = 0;
        for (char c : token.toCharArray()) {
            if (c == '.') dotCount++;
        }

        if (dotCount != 2) {
            log.warn("⚠️ JWT 형식 오류: .이 {}개 (expected 2)", dotCount);
            log.warn("   토큰 샘플: {}", token.substring(0, Math.min(50, token.length())) + "...");
            return null;
        }

        log.debug("✅ 토큰 추출 완료 - 길이: {}", token.length());
        return token;
    }

    /**
     * 구독(SUBSCRIBE) 권한 검증 (선택사항)
     *
     * 현재는 기본 인증만 처리하고, 구독 권한(특정 room 멤버인지)은
     * Controller 레벨에서 처리하는 것이 더 유연함
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            log.warn("WebSocket 메시지 전송 실패");
        }
    }
}
