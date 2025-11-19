package com.sobunsobun.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 위치 인증 요청 DTO
 *
 * 주소 정보만 저장합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationVerificationRequest {
    /**
     * 주소 (필수)
     */
    @NotBlank(message = "주소는 필수입니다.")
    private String address;
}

