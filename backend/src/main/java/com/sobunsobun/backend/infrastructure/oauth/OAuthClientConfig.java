package com.sobunsobun.backend.infrastructure.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
public class OAuthClientConfig {
    @Bean
    public Map<OAuthProvider, OAuthClient> oauthClients(
            KakaoOAuthClientImpl kakao,
            GoogleOAuthClientImpl google,
            AppleOAuthClientImpl apple
    ){
        Map<OAuthProvider, OAuthClient> m = new EnumMap<>(OAuthProvider.class);
        m.put(OAuthProvider.KAKAO, kakao);
        m.put(OAuthProvider.GOOGLE, google);
        m.put(OAuthProvider.APPLE, apple);
        return m;
    }
}
