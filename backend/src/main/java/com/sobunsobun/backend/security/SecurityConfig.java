package com.sobunsobun.backend.security;

import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 보안 설정 클래스
 *
 * 주요 기능:
 * - JWT 기반 인증/인가 설정
 * - CORS 정책 설정
 * - 공개/보호 엔드포인트 구분
 * - 보안 필터 체인 구성
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize, @PostAuthorize 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /** 공개 접근 허용 엔드포인트 패턴 */
    private static final String[] PUBLIC_ENDPOINTS = {
        // API 문서
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",

        // 헬스체크
        "/health",

        // 인증 관련 (모든 하위 경로)
        "/auth/**",

        // 사용자 관련 공개 API
        "/users/**",

        // 정적 파일 접근
        "/files/**"


    };

    /**
     * 빈 UserDetailsService 제공
     *
     * Spring Security 기본 사용자 생성 경고를 제거하기 위해
     * 빈 InMemoryUserDetailsManager를 제공합니다.
     * JWT 기반 인증에서는 실제로 사용되지 않습니다.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(); // 빈 사용자 관리자
    }

    /**
     * 패스워드 인코더 (추후 확장 대비)
     *
     * 현재는 OAuth 로그인만 지원하지만,
     * 향후 일반 회원가입 기능 추가 시 사용할 수 있습니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정
     *
     * 프론트엔드 애플리케이션에서 API 호출을 허용하기 위한 CORS 정책을 설정합니다.
     *
     * 주의사항:
     * - 운영 환경에서는 allowedOrigins를 특정 도메인으로 제한 권장
     * - allowCredentials=false로 설정하여 쿠키 기반 인증 비활성화
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (운영 시 특정 도메인으로 제한 권장)
        configuration.setAllowedOrigins(List.of("*"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 응답에 노출할 헤더 (클라이언트에서 접근 가능)
        configuration.setExposedHeaders(List.of("Authorization"));

        // 쿠키/인증 정보 포함 여부 (JWT 사용으로 false)
        configuration.setAllowCredentials(false);

        // Preflight 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 보안 필터 체인 설정
     *
     * JWT 기반 인증을 위한 Spring Security 설정을 구성합니다.
     *
     * 주요 설정:
     * - CSRF 비활성화 (JWT 사용으로 불필요)
     * - 세션 비활성화 (Stateless)
     * - JWT 인증 필터 추가
     * - 엔드포인트별 접근 권한 설정
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // JWT 인증 필터 생성
        JwtAuthenticationFilter jwtAuthenticationFilter =
            new JwtAuthenticationFilter(jwtTokenProvider, userRepository);

        return http
                // CORS 활성화
                .cors(Customizer.withDefaults())

                // CSRF 비활성화 (JWT 사용으로 불필요)
                .csrf(csrf -> csrf.disable())

                // X-Frame-Options 비활성화 (개발 편의성)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))

                // 세션 정책: Stateless (JWT 사용)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증 실패 시 처리
                .exceptionHandling(exception ->
                    exception.authenticationEntryPoint(new RestAuthEntryPoint()))

                // 엔드포인트별 접근 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                    // 공개 엔드포인트: 인증 없이 접근 가능
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                    // 게시글 조회(GET)는 공개, 생성/수정/삭제는 인증 필요
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts/**").permitAll()

                    // 관리자 전용 API: ADMIN 권한 필요
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // 나머지 모든 요청: 인증 필요
                    .anyRequest().authenticated()
                )

                // 폼 로그인 비활성화 (JWT 사용)
                .formLogin(form -> form.disable())

                // HTTP Basic 인증 비활성화 (JWT만 사용)
                .httpBasic(httpBasic -> httpBasic.disable())

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
