package com.sobunsobun.backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FCM 토큰 활성화/비활성화 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceUpdateResponse {

    /**
     * 디바이스 ID
     */
    private String deviceId;

    /**
     * 활성화 여부
     */
    private Boolean isEnabled;

    /**
     * 변경 일시
     */
    private LocalDateTime updatedAt;
}

