package com.sobunsobun.backend.config.websocket;

import com.sobunsobun.backend.domain.Role;
import com.sobunsobun.backend.security.JwtTokenProvider;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    // 세션 안에 넣어둘 키
    private static final String AUTH_SESSION_KEY = "WS_AUTH";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();

        // 1) CONNECT: 토큰 파싱 + Authentication 생성 + 세션에 저장
        if (StompCommand.CONNECT.equals(command)) {
            var nativeHeaders = accessor.getNativeHeader("Authorization");

            if (nativeHeaders == null || nativeHeaders.isEmpty()) {
                return message;
            }

            String rawAuthHeader = nativeHeaders.get(0);

            String token = extractToken(rawAuthHeader);
            if (token == null) {
                return message;
            }

            Jws<Claims> claimsJws = jwtTokenProvider.parse(token);
            Claims claims = claimsJws.getBody();

            Long userId = Long.valueOf(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));

            JwtUserPrincipal principal = new JwtUserPrincipal(userId, role);
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            accessor.setUser(auth);
            accessor.getSessionAttributes().put(AUTH_SESSION_KEY, auth);
            return message;
        }

        // 2) SEND / SUBSCRIBE 등: 세션에 저장해 둔 auth를 다시 꺼내서 꽂아줌
        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            Object sessionAuth = accessor.getSessionAttributes().get(AUTH_SESSION_KEY);

            if (accessor.getUser() == null && sessionAuth instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
            }
        }

        return message;
    }

    private String extractToken(String rawAuthHeader) {
        if (rawAuthHeader == null) {
            return null;
        }
        if (rawAuthHeader.startsWith("Bearer ")) {
            return rawAuthHeader.substring(7);
        }
        return rawAuthHeader;
    }
}