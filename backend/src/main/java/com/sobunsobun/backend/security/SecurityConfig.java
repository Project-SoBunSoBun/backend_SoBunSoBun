package com.sobunsobun.backend.security;

import com.sobunsobun.backend.infrastructure.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtFilter){ this.jwtFilter = jwtFilter; }

    /**
     * 핵심 보안 설정:
     * - CSRF 비활성(REST/토큰 기반)
     * - 세션 무상태(STATELESS)
     * - 카카오 인증 경로/헬스체크는 permitAll
     * - 그 외는 인증 필요
     * - JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
     * - 개발용 CORS(운영에서는 Origin 제한)
     */

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.csrf(csrf->csrf.disable())
                .cors(c->c.configurationSource(cors()))
                .sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/healthz",
                                "/auth/login/kakao",
                                "/auth/callback/kakao",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS 설정: 개발용으로 전체 허용. 운영 전환 시 반드시 Origin/메서드 제한 권장 */
    @Bean
    public CorsConfigurationSource cors(){
        var cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*");
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
