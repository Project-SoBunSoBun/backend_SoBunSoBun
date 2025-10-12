package com.sobunsobun.backend.infrastructure.oauth;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** 카카오 Access Token -> 사용자 정보 조회 */
@Component
public class KakaoOAuthClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://kapi.kakao.com")
            .build();

    public Mono<KakaoUserResponse> getUserInfo(String kakaoAccessToken) {
        return webClient.get()
                .uri("/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class);
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
