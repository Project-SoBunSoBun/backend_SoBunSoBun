package com.sobunsobun.backend.infrastructure.oauth;

import org.springframework.stereotype.Component;

@Component
public class AppleOAuthClientImpl implements OAuthClient {
    @Override public OAuthProvider provider(){ return OAuthProvider.APPLE; }
    @Override public String buildAuthorizeUrl(String redirectUri, String stateB64){
        // TODO: Apple authorize url 조합
        return "https://appleid.apple.com/auth/authorize?...&redirect_uri="+redirectUri+"&state="+stateB64;
    }
    @Override public String getAccessToken(String code, String redirectUri){
        // TODO: 애플 토큰 교환(서명된 client_secret 필요)
        throw new UnsupportedOperationException("TODO: implement");
    }
    @Override public OAuthUserInfo getUserInfo(String accessToken){
        // TODO: 애플 사용자 조회(이메일 비공개 시 주의)
        throw new UnsupportedOperationException("TODO: implement");
    }
}
