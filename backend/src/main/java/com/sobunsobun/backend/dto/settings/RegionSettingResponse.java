package com.sobunsobun.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 지역 설정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionSettingResponse {

    /**
     * 시/도
     */
    private String sido;

    /**
     * 시/군/구
     */
    private String sigungu;

    /**
     * 동/읍/면
     */
    private String dong;

    /**
     * 전체 주소
     */
    private String fullAddress;

    /**
     * 위도
     */
    private Double latitude;

    /**
     * 경도
     */
    private Double longitude;

    /**
     * 위치 인증 일시
     */
    private LocalDateTime verifiedAt;
}

