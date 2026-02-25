package com.sobunsobun.backend.infrastructure.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * Apple OAuth 클라이언트
 *
 * 담당 기능:
 * - Apple client_secret JWT 생성 (ES256)
 * - Authorization Code → Token 교환
 * - Apple id_token 검증 (RS256, Apple 공개 키)
 */
@Slf4j
@Component
public class AppleOAuthClient {

    @Value("${APPLE_CLIENT_ID}")
    private String clientId;

    @Value("${APPLE_TEAM_ID}")
    private String teamId;

    @Value("${APPLE_KEY_ID}")
    private String keyId;

    @Value("${APPLE_PRIVATE_KEY_PATH}")
    private Resource privateKeyResource;

    @Value("${APPLE_REDIRECT_URI:}")
    private String redirectUri;

    private static final String APPLE_AUTH_URL = "https://appleid.apple.com";
    private static final String APPLE_TOKEN_URL = APPLE_AUTH_URL + "/auth/token";
    private static final String APPLE_KEYS_URL = APPLE_AUTH_URL + "/auth/keys";

    private final WebClient webClient = WebClient.builder().build();

    // Apple 공개 키 캐시 (서버 재시작 시 갱신)
    private JWKSet cachedJwkSet;
    private long jwkSetCachedAt = 0;
    private static final long JWK_CACHE_TTL = 24 * 60 * 60 * 1000L; // 24시간

    /**
     * Apple client_secret JWT 생성
     *
     * Apple은 일반적인 client_secret 문자열 대신
     * ES256으로 서명된 JWT를 client_secret으로 사용합니다.
     *
     * @return ES256 서명된 client_secret JWT
     */
    public String generateClientSecret() {
        try {
            PrivateKey privateKey = loadPrivateKey();
            Date now = new Date();
            Date expiration = new Date(now.getTime() + 5 * 60 * 1000); // 5분 만료

            return Jwts.builder()
                    .setHeaderParam("kid", keyId)
                    .setHeaderParam("alg", "ES256")
                    .setIssuer(teamId)           // Apple Team ID
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .setAudience(APPLE_AUTH_URL)  // https://appleid.apple.com
                    .setSubject(clientId)         // Apple Service ID (client_id)
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();
        } catch (Exception e) {
            log.error("Apple client_secret 생성 실패: {}", e.getMessage());
            throw new RuntimeException("Apple client_secret 생성 실패", e);
        }
    }

    /**
     * Authorization Code → Token 교환
     *
     * Apple의 /auth/token 엔드포인트에 POST 요청으로
     * authorization_code를 id_token으로 교환합니다.
     *
     * @param authorizationCode Apple에서 받은 authorization code
     * @return Apple 토큰 응답 (id_token 포함)
     */
    public AppleTokenResponse exchangeCodeForTokens(String authorizationCode) {
        String clientSecret = generateClientSecret();

        log.info("Apple 토큰 교환 요청 - client_id: {}", clientId);

        return webClient.post()
                .uri(APPLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("code", authorizationCode)
                        .with("grant_type", "authorization_code")
                        .with("redirect_uri", redirectUri))
                .retrieve()
                .bodyToMono(AppleTokenResponse.class)
                .block();
    }

    /**
     * Apple id_token 검증 및 사용자 정보 추출
     *
     * Apple의 공개 키(JWK)를 사용하여 id_token의 서명을 검증하고
     * 사용자 고유 ID(sub)와 이메일을 추출합니다.
     *
     * @param idToken Apple에서 받은 id_token
     * @return 검증된 사용자 정보 (sub, email)
     */
    public AppleUserInfo verifyIdToken(String idToken) {
        try {
            // 1. id_token 파싱
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            String kid = signedJWT.getHeader().getKeyID();
            JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();

            log.debug("Apple id_token 검증 시작 - kid: {}, alg: {}", kid, algorithm);

            // 2. Apple 공개 키 가져오기
            JWKSet jwkSet = getApplePublicKeys();
            JWK matchingKey = jwkSet.getKeyByKeyId(kid);

            if (matchingKey == null) {
                // 캐시된 키에 없으면 강제 갱신
                log.info("Apple 공개 키 캐시 미스 - kid: {}, 키 갱신 시도", kid);
                cachedJwkSet = null;
                jwkSet = getApplePublicKeys();
                matchingKey = jwkSet.getKeyByKeyId(kid);
            }

            if (matchingKey == null) {
                throw new RuntimeException("Apple 공개 키를 찾을 수 없습니다. kid: " + kid);
            }

            // 3. RSA 공개 키로 서명 검증
            RSAPublicKey publicKey = ((RSAKey) matchingKey).toRSAPublicKey();
            com.nimbusds.jose.crypto.RSASSAVerifier verifier =
                    new com.nimbusds.jose.crypto.RSASSAVerifier(publicKey);

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Apple id_token 서명 검증 실패");
            }

            // 4. 클레임 추출 및 검증
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // issuer 검증
            if (!APPLE_AUTH_URL.equals(claims.getIssuer())) {
                throw new RuntimeException("Apple id_token issuer 불일치: " + claims.getIssuer());
            }

            // audience 검증
            if (!claims.getAudience().contains(clientId)) {
                throw new RuntimeException("Apple id_token audience 불일치");
            }

            // 만료 검증
            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("Apple id_token 만료됨");
            }

            // 5. 사용자 정보 추출
            String sub = claims.getSubject();             // Apple 사용자 고유 ID
            String email = (String) claims.getClaim("email");
            Boolean emailVerified = (Boolean) claims.getClaim("email_verified");

            log.info("Apple id_token 검증 완료 - sub: {}, email: {}", sub, email);

            AppleUserInfo userInfo = new AppleUserInfo();
            userInfo.setSub(sub);
            userInfo.setEmail(email);
            userInfo.setEmailVerified(emailVerified != null ? emailVerified : false);

            return userInfo;

        } catch (Exception e) {
            log.error("Apple id_token 검증 실패: {}", e.getMessage());
            throw new RuntimeException("Apple id_token 검증 실패", e);
        }
    }

    /**
     * Apple 공개 키(JWK Set) 가져오기 (캐시 적용)
     */
    private JWKSet getApplePublicKeys() {
        long now = System.currentTimeMillis();
        if (cachedJwkSet != null && (now - jwkSetCachedAt) < JWK_CACHE_TTL) {
            return cachedJwkSet;
        }

        try {
            log.info("Apple 공개 키 갱신 - URL: {}", APPLE_KEYS_URL);
            cachedJwkSet = JWKSet.load(new URL(APPLE_KEYS_URL));
            jwkSetCachedAt = now;
            log.info("Apple 공개 키 갱신 완료 - 키 수: {}", cachedJwkSet.getKeys().size());
            return cachedJwkSet;
        } catch (Exception e) {
            log.error("Apple 공개 키 조회 실패: {}", e.getMessage());
            throw new RuntimeException("Apple 공개 키 조회 실패", e);
        }
    }

    /**
     * Apple .p8 개인 키 파일 로드
     * classpath: 또는 file: 접두사로 Resource를 지정할 수 있습니다.
     * 예) classpath:apple/AuthKey_XXXXXXXXXX.p8
     */
    private PrivateKey loadPrivateKey() throws Exception {
        if (!privateKeyResource.exists()) {
            throw new IllegalStateException(
                    "Apple 개인 키 파일을 찾을 수 없습니다: " + privateKeyResource.getDescription()
                    + " (.env의 APPLE_PRIVATE_KEY_PATH를 확인하세요)");
        }
        try (Reader reader = new InputStreamReader(privateKeyResource.getInputStream())) {
            PEMParser pemParser = new PEMParser(reader);
            PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPrivateKey(privateKeyInfo);
        }
    }

    // ========== Inner DTOs ==========

    /**
     * Apple /auth/token 응답
     */
    @Data
    public static class AppleTokenResponse {
        private String access_token;
        private String token_type;
        private Long expires_in;
        private String refresh_token;
        private String id_token;
    }

    /**
     * Apple id_token에서 추출된 사용자 정보
     */
    @Data
    public static class AppleUserInfo {
        private String sub;              // Apple 사용자 고유 ID (oauthId)
        private String email;
        private boolean emailVerified;
    }
}
