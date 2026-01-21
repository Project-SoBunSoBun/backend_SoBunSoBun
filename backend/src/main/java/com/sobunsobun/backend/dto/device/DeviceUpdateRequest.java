package com.sobunsobun.backend.dto.device;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FCM 토큰 활성화/비활성화 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceUpdateRequest {

    /**
     * 활성화 여부 (필수)
     */
    @NotNull(message = "활성화 여부는 필수입니다.")
    private Boolean isEnabled;
}

