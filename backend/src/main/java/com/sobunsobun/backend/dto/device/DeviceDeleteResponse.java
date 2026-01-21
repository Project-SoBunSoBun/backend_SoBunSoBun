package com.sobunsobun.backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 삭제 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceDeleteResponse {

    /**
     * 디바이스 ID
     */
    private String deviceId;

    /**
     * 삭제 성공 여부
     */
    private Boolean deleted;
}

