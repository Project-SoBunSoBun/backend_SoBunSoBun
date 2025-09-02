package com.sobunsobun.backend.infrastructure.oauth;

import com.sobunsobun.backend.dto.auth.KakaoTokenResponse;
import com.sobunsobun.backend.dto.auth.KakaoUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoOAuthClient {

    @Value("${oauth2.kakao.client-id}")     private String clientId;
    @Value("${oauth2.kakao.client-secret:}")private String clientSecret;
    @Value("${oauth2.kakao.redirect-uri}")  private String redirectUri;

    private final RestTemplate rest = new RestTemplate();

    public KakaoTokenResponse exchangeCodeForToken(String code) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
        body.add("grant_type","authorization_code");
        body.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty()) body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        return rest.postForEntity("https://kauth.kakao.com/oauth/token",
                new HttpEntity<>(body, h), KakaoTokenResponse.class).getBody();
    }

    public KakaoUserResponse getUser(String kakaoAccessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(kakaoAccessToken);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return rest.postForEntity("https://kapi.kakao.com/v2/user/me",
                new HttpEntity<>(h), KakaoUserResponse.class).getBody();
    }
}
