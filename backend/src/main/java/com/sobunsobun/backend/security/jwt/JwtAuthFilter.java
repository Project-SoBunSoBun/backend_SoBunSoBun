package com.sobunsobun.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jws<Claims> jws = jwt.parse(token);
                Claims c = jws.getBody();
                if (!jwt.isRefreshToken(c)) {
                    String role = (String) c.get("role");
                    List<GrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : Collections.emptyList();
                    UserPrincipal principal = new UserPrincipal(
                            Long.parseLong(c.getSubject()),
                            (String) c.get("provider"),
                            (String) c.get("nickname"),
                            role
                    );
                    Authentication authentication =
                            new UsernamePasswordAuthenticationToken(principal, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignore) {}
        }
        chain.doFilter(request, response);
    }

    public static class UserPrincipal {
        public final long userId;
        public final String provider;
        public final String nickname;
        public final String role;
        public UserPrincipal(long userId, String provider, String nickname, String role) {
            this.userId = userId; this.provider = provider; this.nickname = nickname; this.role = role;
        }
    }
}
