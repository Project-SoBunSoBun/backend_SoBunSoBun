package com.sobunsobun.backend.infrastructure.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class KakaoOAuthClient {

    private final WebClient webClient;
    private final String tokenUri;
    private final String userinfoUri;
    private final String clientId;
    private final String clientSecret;

    public KakaoOAuthClient(
            WebClient.Builder builder,
            @Value("${oauth.kakao.token-uri}") String tokenUri,
            @Value("${oauth.kakao.userinfo-uri}") String userinfoUri,
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.client-secret:}") String clientSecret
    ) {
        this.webClient = builder.build();
        this.tokenUri = tokenUri;
        this.userinfoUri = userinfoUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public KakaoTokenResponse exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        return webClient.get()
                .uri(userinfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}
