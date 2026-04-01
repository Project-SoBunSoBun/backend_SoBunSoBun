package com.sobunsobun.backend.dto.auth;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class KakaoVerifyResponse {
    private boolean success; // 인증 성공 여부
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String loginToken; // 임시 토큰 (이용약관 동의용)
    private boolean isNewUser; // 신규 가입자 여부
}
