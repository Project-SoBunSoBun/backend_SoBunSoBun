package com.sobunsobun.backend.infrastructure.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 인프라(기술) 계층:
 * - HS256 대칭키 기반으로 JWT 생성/검증
 * - 시크릿/만료기간은 application.yml에서 주입(@Value)
 */

@Component
public class JwtTokenProvider {
    // application.yml: security.jwt.secret
    @Value("${security.jwt.secret}") private String secret;

    // application.yml: security.jwt.access-token-validity (ms)
    @Value("${security.jwt.access-token-validity}") private long accessValidityMs;

    // application.yml: security.jwt.refresh-token-validity (ms)
    @Value("${security.jwt.refresh-token-validity}") private long refreshValidityMs;

    // HMAC 서명에 사용할 바이트 키
    private byte[] key;

    /** 앱 시작 시 문자열 시크릿을 바이트 키로 변환 */
    @PostConstruct void init(){
        //운영에서는 32바이트 이상 강한 랜덤 문자열을 환경변수로 주입
        key = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Access 토큰 생성: userId를 sub 클레임에, 권한은 role 클레임에 담는다 */
    public String createAccessToken(Long userId, String role) {
        Date now = new Date(); Date exp = new Date(now.getTime()+accessValidityMs);
        return Jwts.builder().setSubject(String.valueOf(userId))
                .claim("role", role).setIssuedAt(now).setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    /** Refresh 토큰 생성: 최소 정보(sub, exp)만 포함 */
    public String createRefreshToken(Long userId) {
        Date now = new Date(); Date exp = new Date(now.getTime()+refreshValidityMs);
        return Jwts.builder().setSubject(String.valueOf(userId))
                .setIssuedAt(now).setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    /** 토큰 파싱/검증: 서명/만료 체크 후 Claims 반환 (위조/만료 시 예외 발생) */
    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(key)).build()
                .parseClaimsJws(token).getBody();// 유효하지 않으면 예외(JwtException) 던짐
    }
}
