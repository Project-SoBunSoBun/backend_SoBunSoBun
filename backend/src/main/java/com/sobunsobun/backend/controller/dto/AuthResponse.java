package com.sobunsobun.backend.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuthResponse {
    @Schema(description = "우리 서비스 Access JWT")
    private String accessToken;
    @Schema(description = "우리 서비스 Refresh JWT")
    private String refreshToken;
    @Schema(description = "신규 가입 여부")
    private boolean newUser;
    private UserDto user;

    @Schema(description = "iOS 참고용(선택 반환)")
    private KakaoTokenDto kakao;

    @Data @Builder
    public static class UserDto {
        private long id;
        private String nickname;
        private String profileImageUrl;
        private String provider; // KAKAO
    }

    @Data @Builder
    public static class KakaoTokenDto {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
    }
}
