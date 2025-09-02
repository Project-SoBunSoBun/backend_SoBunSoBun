package com.sobunsobun.backend.infrastructure.oauth;

public record OAuthUserInfo(
        String providerId,   // ex) kakao:123, google:abc, apple:def
        String email,
        String nickname,
        String profileImage
) {}

