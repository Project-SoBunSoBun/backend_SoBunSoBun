package com.sobunsobun.backend.dto.auth;

import lombok.Builder; import lombok.Getter;
@Getter @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
}
