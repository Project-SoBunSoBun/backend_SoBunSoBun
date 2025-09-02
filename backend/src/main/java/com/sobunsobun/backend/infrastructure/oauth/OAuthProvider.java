package com.sobunsobun.backend.infrastructure.oauth;

public enum OAuthProvider {
    KAKAO, GOOGLE, APPLE;

    public static OAuthProvider from(String s){
        if (s == null) throw new IllegalArgumentException("provider required");
        return OAuthProvider.valueOf(s.toUpperCase());
    }
}
