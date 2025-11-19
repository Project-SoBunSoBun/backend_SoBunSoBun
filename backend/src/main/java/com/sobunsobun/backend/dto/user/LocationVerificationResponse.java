package com.sobunsobun.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 위치 인증 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationVerificationResponse {
    /**
     * 주소
     */
    private String address;

    /**
     * 위치 인증 일시 (마지막 위치 인증 시간)
     */
    private LocalDateTime locationVerifiedAt;

    /**
     * 위치 인증 여부
     */
    private boolean isVerified;

    /**
     * 위치 인증 만료 여부 (24시간 기준)
     */
    private boolean isExpired;

    /**
     * 위치 인증 만료까지 남은 시간 (분 단위)
     */
    private Long remainingMinutes;
}

