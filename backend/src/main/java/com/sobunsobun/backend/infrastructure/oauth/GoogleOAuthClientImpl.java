package com.sobunsobun.backend.infrastructure.oauth;

import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthClientImpl implements OAuthClient {
    @Override public OAuthProvider provider(){ return OAuthProvider.GOOGLE; }
    @Override public String buildAuthorizeUrl(String redirectUri, String stateB64){
        // TODO: Google authorize url 조합
        return "https://accounts.google.com/o/oauth2/v2/auth?...&redirect_uri="+redirectUri+"&state="+stateB64;
    }
    @Override public String getAccessToken(String code, String redirectUri){
        // TODO: 구글 토큰 교환
        throw new UnsupportedOperationException("TODO: implement");
    }
    @Override public OAuthUserInfo getUserInfo(String accessToken){
        // TODO: 구글 사용자 조회
        throw new UnsupportedOperationException("TODO: implement");
    }
}
