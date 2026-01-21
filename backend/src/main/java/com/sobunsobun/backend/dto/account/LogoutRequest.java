package com.sobunsobun.backend.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequest {

    /**
     * 현재 디바이스 ID (FCM 토큰 비활성화용)
     */
    @NotBlank(message = "디바이스 ID는 필수입니다.")
    @Size(max = 100, message = "디바이스 ID는 최대 100자입니다.")
    private String deviceId;
}

