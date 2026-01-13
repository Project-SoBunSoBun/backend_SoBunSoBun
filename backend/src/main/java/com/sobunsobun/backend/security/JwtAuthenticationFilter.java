package com.sobunsobun.backend.security;

import com.sobunsobun.backend.domain.Role;
import com.sobunsobun.backend.repository.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 *
 * Spring Security 필터 체인에서 JWT 토큰을 검증하고
 * SecurityContext에 인증 정보를 설정하는 필터입니다.
 *
 * 동작 과정:
 * 1. Authorization 헤더에서 Bearer 토큰 추출
 * 2. JWT 토큰 유효성 검증 (서명, 만료시간)
 * 3. 토큰에서 사용자 정보 추출
 * 4. 데이터베이스에서 사용자 존재 확인
 * 5. SecurityContext에 Authentication 설정
 *
 * 특징:
 * - OncePerRequestFilter 상속으로 요청당 한 번만 실행
 * - 토큰 오류 시 예외를 발생시키지 않고 인증 실패로 처리
 * - 액세스 토큰만 처리 (리프레시 토큰은 별도 처리)
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /** Bearer 토큰 접두사 */
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    /**
     * JWT 인증 필터 핵심 로직
     *
     * 모든 HTTP 요청에 대해 JWT 토큰을 확인하고 인증 정보를 설정합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException I/O 예외
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String jwtToken = extractTokenFromRequest(request);

            if (jwtToken != null) {
                log.info("🔑 JWT 토큰 발견 - URI: {}, 토큰 길이: {}", request.getRequestURI(), jwtToken.length());

                // 2. JWT 토큰 파싱 및 검증
                Claims claims = jwtTokenProvider.parse(jwtToken).getBody();

                // 3. 토큰 타입 확인 (액세스 토큰만 허용)
                String tokenType = claims.get("type", String.class);
                if (!"access".equals(tokenType)) {
                    log.warn("❌ 잘못된 토큰 타입: {} - URI: {}", tokenType, request.getRequestURI());
                    return; // 인증 실패로 처리
                }

                // 4. 사용자 정보 추출
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);

                log.info("✅ JWT 토큰 검증 성공 - 사용자 ID: {}, 역할: {}", userId, role);

                // 5. 사용자 존재 확인 (선택적, 보안 강화)
                if (!userRepository.existsById(userId)) {
                    log.warn("❌ 토큰의 사용자 ID가 DB에 존재하지 않음: {}", userId);
                    return; // 인증 실패로 처리
                }

                // 6. SecurityContext에 Authentication 설정
                setAuthenticationInSecurityContext(userId, role);

                log.info("✅ 인증 성공 - 사용자 ID: {}, URI: {}", userId, request.getRequestURI());
            } else {
                log.info("⚠️ JWT 토큰 없음 - URI: {}", request.getRequestURI());
            }

        } catch (Exception e) {
            // JWT 관련 모든 예외를 캐치하여 인증 실패로 처리
            log.warn("❌ JWT 인증 실패 - {}: {} - URI: {}",
                    e.getClass().getSimpleName(), e.getMessage(), request.getRequestURI());
        }

        // 다음 필터로 진행 (인증 성공/실패 무관하게 진행)
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     *
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열 (없으면 null)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authorizationHeader)) {
            if (authorizationHeader.startsWith(BEARER_PREFIX)) {
                String token = authorizationHeader.substring(BEARER_PREFIX_LENGTH);
                log.info("📥 Authorization 헤더에서 토큰 추출 성공 (길이: {})", token.length());
                return token;
            } else {
                log.warn("⚠️ Authorization 헤더가 Bearer로 시작하지 않음: {}",
                        authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())));
            }
        } else {
            log.info("ℹ️ Authorization 헤더 없음");
        }

        return null;
    }

    /**
     * SecurityContext에 인증 정보 설정
     *
     * 검증된 사용자 정보로 Authentication 객체를 생성하고
     * SecurityContext에 설정합니다.
     *
     * @param userId 사용자 ID
     * @param role 사용자 역할
     */
    private void setAuthenticationInSecurityContext(Long userId, String role) {
        // JWT 사용자 정보 객체 생성
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, Role.valueOf(role));

        // Spring Security 권한 객체 생성 (ROLE_ 접두사 추가)
        List<SimpleGrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority("ROLE_" + role));

        // Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal,      // Principal: 인증된 사용자 정보
            null,          // Credentials: JWT 사용 시 불필요
            authorities    // Authorities: 사용자 권한 목록
        );

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("SecurityContext에 인증 정보 설정 완료 - 사용자 ID: {}, 권한: {}",
                userId, authorities);
    }

    /**
     * 필터를 적용할지 여부 결정
     *
     * 특정 경로에 대해 필터를 건너뛰고 싶은 경우 오버라이드하여 사용합니다.
     * 현재는 모든 요청에 대해 필터를 적용합니다.
     *
     * @param request HTTP 요청
     * @return true면 필터 적용, false면 건너뛰기
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 현재는 모든 요청에 대해 필터 적용
        // 필요시 특정 경로 제외 로직 추가 가능
        return false;
    }
}
