package com.sobunsobun.backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FCM 토큰 등록/갱신 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistrationResponse {

    /**
     * 디바이스 ID
     */
    private String deviceId;

    /**
     * 등록 성공 여부
     */
    private Boolean registered;

    /**
     * 활성화 여부
     */
    private Boolean isEnabled;

    /**
     * 등록/갱신 일시
     */
    private LocalDateTime registeredAt;
}

