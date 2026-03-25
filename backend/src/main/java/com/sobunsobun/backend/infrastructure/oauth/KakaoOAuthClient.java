package com.sobunsobun.backend.infrastructure.oauth;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** 카카오 OAuth 클라이언트 */
@Component
public class KakaoOAuthClient {

    @Value("${KAKAO_REST_API_KEY}")
    private String restApiKey;

    @Value("${KAKAO_REDIRECT_URI}")
    private String redirectUri;

    private final WebClient apiClient = WebClient.builder()
            .baseUrl("https://kapi.kakao.com")
            .build();

    private final WebClient authClient = WebClient.builder()
            .baseUrl("https://kauth.kakao.com")
            .build();

    /** Authorization Code → Access Token 교환 */
    public KakaoTokenResponse exchangeCodeForAccessToken(String code) {
        return authClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", restApiKey)
                        .with("redirect_uri", redirectUri)
                        .with("code", code))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    /** Access Token → 사용자 정보 조회 */
    public Mono<KakaoUserResponse> getUserInfo(String kakaoAccessToken) {
        return apiClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class);
    }

    @Data
    public static class KakaoTokenResponse {
        private String token_type;
        private String access_token;
        private Integer expires_in;
        private String refresh_token;
        private Integer refresh_token_expires_in;
    }

    @Data
    public static class KakaoUserResponse {
        private Long id; // oauth_id
        private KakaoAccount kakao_account;

        @Data
        public static class KakaoAccount {
            private String email; // 동의 받지 못하면 null
        }
    }
}
