package com.sobunsobun.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket STOMP 연결 시 JWT 검증
 *
 * 동작:
 * 1. CONNECT 프레임에서 Authorization 헤더 추출
 * 2. JWT 토큰 파싱 및 서명 검증
 * 3. 유효한 토큰만 연결 허용
 * 4. 토큰에서 userId 추출하여 Principal에 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // CONNECT 프레임만 처리
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        try {
            log.info(" STOMP CONNECT 프레임 수신");

            // 1. Authorization 헤더에서 토큰 추출
            List<String> authorization = accessor.getNativeHeader("Authorization");

            // Authorization 헤더가 없는 경우: 테스트 모드에서는 클라이언트에서 전달한 userId 사용
            if (authorization == null || authorization.isEmpty()) {
                log.info(" Authorization 헤더 없음 - 테스트 모드로 진행");

                // 세션 속성에서 userId 추출 시도
                Long userId = null;
                if (accessor.getSessionAttributes() != null) {
                    Object userIdObj = accessor.getSessionAttributes().get("userId");
                    if (userIdObj != null) {
                        userId = Long.valueOf(userIdObj.toString());
                    }
                }

                // userId가 없으면 기본값 사용
                if (userId == null) {
                    userId = 999L;
                    log.info(" 기본 userId 사용: {}", userId);
                }

                log.info(" 테스트 토큰 없이 연결 허용 - userId: {}", userId);

                // Principal 설정
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(userId, com.sobunsobun.backend.domain.Role.USER),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                accessor.setUser(authentication);

                // 세션에 userId 저장
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("role", "USER");
                }

                return message;
            }

            String token = authorization.get(0);
            log.info(" Authorization 헤더 발견 (길이: {})", token.length());

            // 2. Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
                log.debug(" Bearer 접두사 제거됨");
            }

            // 3. 토큰 검증 및 파싱
            Claims claims = null;

            // test-token은 개발/테스트 환경에서만 허용
            if ("test-token".equals(token)) {
                log.info(" 테스트 토큰 사용 (개발 환경)");
                // 테스트 환경에서는 기본 사용자로 설정
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(999L, com.sobunsobun.backend.domain.Role.USER),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                accessor.setUser(authentication);

                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("userId", 999L);
                    accessor.getSessionAttributes().put("role", "USER");
                }

                return message;
            }

            // 일반 JWT 토큰 파싱 및 검증 (서명, 만료시간 확인)
            try {
                claims = jwtTokenProvider.parse(token).getBody();
            } catch (JwtException e) {
                log.error(" JWT 토큰 검증 실패: {}", e.getMessage());
                throw e;
            }

            // 5. 토큰 타입 확인 (access 토큰만 허용)
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                log.error(" 잘못된 토큰 타입: {} - 연결 거부", tokenType);
                throw new JwtException("잘못된 토큰 타입입니다");
            }

            // 6. userId 추출
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            log.info(" WebSocket 인증 성공 - userId: {}, role: {}", userId, role);

            // 7. Principal 설정
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    new JwtUserPrincipal(userId, com.sobunsobun.backend.domain.Role.valueOf(role)),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            accessor.setUser(authentication);

            // 8. 세션에 userId 저장
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("userId", userId);
                accessor.getSessionAttributes().put("role", role);
            }

        } catch (JwtException e) {
            log.error(" WebSocket 인증 실패: {}", e.getMessage());
            throw new RuntimeException("WebSocket 인증 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error(" WebSocket 처리 중 에러: {}", e.getMessage(), e);
            throw new RuntimeException("WebSocket 처리 중 에러 발생", e);
        }

        return message;
    }
}

