package com.sobunsobun.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 유틸리티 클래스
 *
 * 주요 기능:
 * - JWT 액세스 토큰 생성 (사용자 ID, 역할 포함)
 * - JWT 리프레시 토큰 생성 (사용자 ID만 포함)
 * - 임시 로그인 토큰 생성 (이용약관 동의용)
 * - JWT 토큰 파싱 및 검증
 *
 * 보안 정책:
 * - HMAC-SHA256 서명 알고리즘 사용
 * - 환경변수에서 비밀키 주입
 * - 토큰 타입별 클레임 분리
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final Key signingKey;

    /**
     * JWT 서명용 비밀키 초기화
     *
     * @param secretKey 환경변수 JWT_SECRET에서 주입되는 비밀키
     */
    public JwtTokenProvider(@Value("${JWT_SECRET}") String secretKey) {
        // UTF-8 인코딩으로 비밀키를 바이트 배열로 변환 후 HMAC 키 생성
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("JWT TokenProvider 초기화 완료");
    }

    /**
     * 액세스 토큰 생성
     *
     * 사용자 인증 및 API 호출에 사용되는 단기 토큰입니다.
     *
     * 포함 정보:
     * - subject: 사용자 ID
     * - role: 사용자 역할 (USER, ADMIN)
     * - iat: 발급 시간
     * - exp: 만료 시간
     *
     * @param userId 사용자 ID (문자열)
     * @param role 사용자 역할
     * @param ttlMillis 토큰 유효 시간 (밀리초)
     * @return 생성된 JWT 액세스 토큰
     */
    public String createAccessToken(String userId, String role, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);

        String token = Jwts.builder()
                .setSubject(userId)                           // 사용자 ID
                .claim("role", role)                          // 사용자 역할
                .claim("type", "access")                      // 토큰 타입
                .setIssuedAt(now)                            // 발급 시간
                .setExpiration(expiration)                   // 만료 시간
                .signWith(signingKey, SignatureAlgorithm.HS256) // HMAC-SHA256 서명
                .compact();

        log.debug("액세스 토큰 생성 완료 - 사용자 ID: {}, 역할: {}, 만료: {}",
                userId, role, expiration);

        return token;
    }

    /**
     * 리프레시 토큰 생성
     *
     * 액세스 토큰 갱신에 사용되는 장기 토큰입니다.
     * 보안을 위해 최소한의 정보만 포함합니다.
     *
     * 포함 정보:
     * - subject: 사용자 ID
     * - iat: 발급 시간
     * - exp: 만료 시간
     *
     * @param userId 사용자 ID (문자열)
     * @param ttlMillis 토큰 유효 시간 (밀리초)
     * @return 생성된 JWT 리프레시 토큰
     */
    public String createRefreshToken(String userId, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);

        String token = Jwts.builder()
                .setSubject(userId)                           // 사용자 ID
                .claim("type", "refresh")                     // 토큰 타입
                .setIssuedAt(now)                            // 발급 시간
                .setExpiration(expiration)                   // 만료 시간
                .signWith(signingKey, SignatureAlgorithm.HS256) // HMAC-SHA256 서명
                .compact();

        log.debug("리프레시 토큰 생성 완료 - 사용자 ID: {}, 만료: {}", userId, expiration);

        return token;
    }

    /**
     * 임시 로그인 토큰 생성 (이용약관 동의용)
     *
     * 카카오 토큰 검증 후 이용약관 동의까지의 중간 단계에서 사용되는 임시 토큰입니다.
     *
     * 포함 정보:
     * - subject: "login_temp" (고정값)
     * - email: 사용자 이메일
     * - oauthId: 카카오 OAuth ID
     * - type: "login" (토큰 타입)
     * - iat: 발급 시간
     * - exp: 만료 시간 (10분)
     *
     * @param email 사용자 이메일
     * @param oauthId 카카오 OAuth ID
     * @param ttlMillis 토큰 유효 시간 (밀리초)
     * @return 생성된 임시 로그인 토큰
     */
    public String createLoginToken(String email, String oauthId, long ttlMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlMillis);

        String token = Jwts.builder()
                .setSubject("login_temp")                     // 임시 토큰 식별자
                .claim("email", email)                        // 사용자 이메일
                .claim("oauthId", oauthId)                   // 카카오 OAuth ID
                .claim("type", "login")                       // 토큰 타입
                .setIssuedAt(now)                            // 발급 시간
                .setExpiration(expiration)                   // 만료 시간
                .signWith(signingKey, SignatureAlgorithm.HS256) // HMAC-SHA256 서명
                .compact();

        log.debug("임시 로그인 토큰 생성 완료 - 이메일: {}, 만료: {}", email, expiration);

        return token;
    }

    /**
     * JWT 토큰 파싱 및 검증
     *
     * 토큰의 서명, 만료 시간, 형식을 검증하고 클레임을 추출합니다.
     *
     * 검증 항목:
     * - 서명 유효성 (HMAC-SHA256)
     * - 만료 시간
     * - 토큰 형식
     *
     * @param token 검증할 JWT 토큰
     * @return 파싱된 JWT Claims (헤더 + 페이로드)
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Jws<Claims> parse(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)               // 서명 검증용 키 설정
                    .build()
                    .parseClaimsJws(token);                  // 토큰 파싱 및 검증

            log.debug("JWT 토큰 파싱 성공 - Subject: {}", claimsJws.getBody().getSubject());

            return claimsJws;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("JWT 서명 검증 실패: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 빈 값입니다: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * JWT 토큰 유효성 간단 확인
     *
     * 예외를 발생시키지 않고 boolean으로 유효성을 반환합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 유효하면 true, 유효하지 않으면 false
     */
    public boolean isTokenValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT 토큰 유효성 검사 실패 {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
