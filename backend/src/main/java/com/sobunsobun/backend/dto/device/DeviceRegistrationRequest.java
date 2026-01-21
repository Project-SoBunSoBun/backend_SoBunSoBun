package com.sobunsobun.backend.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 등록/갱신 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationRequest {

    /**
     * 디바이스 고유 ID (필수, UUID)
     */
    @NotBlank(message = "디바이스 ID는 필수입니다.")
    @Size(max = 100, message = "디바이스 ID는 최대 100자입니다.")
    private String deviceId;

    /**
     * FCM 토큰 (필수)
     */
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Size(max = 500, message = "FCM 토큰은 최대 500자입니다.")
    private String fcmToken;

    /**
     * 플랫폼 (필수, IOS 또는 ANDROID)
     */
    @NotNull(message = "플랫폼은 필수입니다.")
    @Pattern(regexp = "IOS|ANDROID", message = "플랫폼은 IOS 또는 ANDROID여야 합니다.")
    private String platform;

    /**
     * 앱 버전 (선택)
     */
    @Size(max = 20, message = "앱 버전은 최대 20자입니다.")
    private String appVersion;

    /**
     * OS 버전 (선택)
     */
    @Size(max = 20, message = "OS 버전은 최대 20자입니다.")
    private String osVersion;
}

