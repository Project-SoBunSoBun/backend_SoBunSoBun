package com.sobunsobun.backend.infrastructure.oauth;

public interface OAuthClient {
    OAuthProvider provider();
    String buildAuthorizeUrl(String redirectUri, String stateB64);
    String getAccessToken(String code, String redirectUri); // provider access token string
    OAuthUserInfo getUserInfo(String accessToken);          // normalized user info
}
