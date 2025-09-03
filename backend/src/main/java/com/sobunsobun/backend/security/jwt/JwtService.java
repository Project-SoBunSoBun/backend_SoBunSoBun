package com.sobunsobun.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final Key key;
    private final long accessMinutes;
    private final long refreshDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-minutes:30}") long accessMinutes,
            @Value("${jwt.refresh-days:60}") long refreshDays
    ) {
        byte[] keyBytes;
        try { keyBytes = Decoders.BASE64.decode(secret); }
        catch (Exception e) { keyBytes = secret.getBytes(); }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public String issueAccess(long userId, String role, String provider, String nickname) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .addClaims(Map.of(
                        "role", role,
                        "provider", provider,
                        "nickname", nickname,
                        "tokenType", "access"
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String issueRefresh(long userId, String role, String provider, String nickname) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(Long.toString(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .addClaims(Map.of(
                        "role", role,
                        "provider", provider,
                        "nickname", nickname,
                        "tokenType", "refresh"
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public boolean isRefreshToken(Claims claims) {
        Object t = claims.get("tokenType");
        return "refresh".equals(t);
    }
}
