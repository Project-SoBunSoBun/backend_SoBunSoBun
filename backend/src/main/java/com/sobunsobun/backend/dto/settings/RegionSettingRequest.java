package com.sobunsobun.backend.dto.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지역 설정 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionSettingRequest {

    /**
     * 시/도 (필수)
     */
    @NotBlank(message = "시/도는 필수 입력입니다.")
    @Size(max = 20, message = "시/도는 최대 20자입니다.")
    private String sido;

    /**
     * 시/군/구 (필수)
     */
    @NotBlank(message = "시/군/구는 필수 입력입니다.")
    @Size(max = 20, message = "시/군/구는 최대 20자입니다.")
    private String sigungu;

    /**
     * 동/읍/면 (필수)
     */
    @NotBlank(message = "동/읍/면은 필수 입력입니다.")
    @Size(max = 20, message = "동/읍/면은 최대 20자입니다.")
    private String dong;

    /**
     * 위도 (필수, -90.0 ~ 90.0)
     */
    @NotNull(message = "위도는 필수 입력입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90.0 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90.0 이하여야 합니다.")
    private Double latitude;

    /**
     * 경도 (필수, -180.0 ~ 180.0)
     */
    @NotNull(message = "경도는 필수 입력입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180.0 이하여야 합니다.")
    private Double longitude;
}

