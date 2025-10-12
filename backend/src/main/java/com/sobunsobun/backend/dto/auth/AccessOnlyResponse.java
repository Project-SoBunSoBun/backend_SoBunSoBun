package com.sobunsobun.backend.dto.auth;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AccessOnlyResponse {
    private String tokenType;   // "Bearer"
    private String accessToken; // 새 Access 토큰
    private long   expiresIn;   // 초 단위 (예: 1800)
}
