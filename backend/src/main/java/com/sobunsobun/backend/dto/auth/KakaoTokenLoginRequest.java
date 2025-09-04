package com.sobunsobun.backend.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class KakaoTokenLoginRequest {
    @NotBlank
    private String accessToken; // iOS가 보내는 카카오 액세스 토큰
}
