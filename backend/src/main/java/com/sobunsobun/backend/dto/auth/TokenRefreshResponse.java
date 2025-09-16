package com.sobunsobun.backend.dto.auth;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken; // 선택: 새로 재발급(여기서는 매번 재발급)
}
