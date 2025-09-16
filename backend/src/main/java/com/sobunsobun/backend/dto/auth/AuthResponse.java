package com.sobunsobun.backend.dto.auth;

import com.sobunsobun.backend.domain.Role;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AuthResponse {
    private String accessToken;   // 우리 서버 JWT (짧은 만료)
    private String refreshToken;  // 우리 서버 Refresh JWT (긴 만료)
    private UserSummary user;

    private String accessTokenExpiresAtKst;
    private String refreshTokenExpiresAtKst;

    @Getter @Setter @Builder
    @AllArgsConstructor @NoArgsConstructor
    public static class UserSummary {
        private Long id;
        private String email;
        private String nickname;
        private String profileImageUrl;
        private Role role;
    }
}
