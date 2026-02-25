package com.sobunsobun.backend.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * Apple 로그인 요청 DTO
 * iOS 앱에서 Apple SDK로 받은 authorization code 또는 id_token을 전달
 */
@Data
public class AppleLoginRequest {
    @NotBlank(message = "authorization code 또는 id_token은 필수입니다.")
    private String code;           // Apple authorization code

    private String idToken;        // Apple id_token (선택 - 앱에서 직접 전달 시)
}
