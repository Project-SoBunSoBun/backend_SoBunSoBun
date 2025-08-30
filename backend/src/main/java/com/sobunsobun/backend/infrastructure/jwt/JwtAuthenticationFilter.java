package com.sobunsobun.backend.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 인프라(기술) 계층:
 * - 매 HTTP 요청마다 Authorization: Bearer <token> 헤더를 검사
 * - 유효한 토큰이면 SecurityContext에 인증 객체 저장 → 컨트롤러에서 Authentication 주입 가능
 * - 유효하지 않으면 인증 없이 통과(보호 경로는 SecurityConfig가 401 반환)
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    public JwtAuthenticationFilter(JwtTokenProvider jwt){ this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(
                        c.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class))));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {}
            // 토큰 위/변조, 만료 등 문제 시 인증 없이 진행
            // (보호된 경로는 SecurityConfig에 의해 최종 401/403 응답)
        }
        chain.doFilter(req, res);
    }
}
