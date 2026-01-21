package com.sobunsobun.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 설정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingResponse {

    /**
     * 전체 푸시 알림 ON/OFF
     */
    private Boolean pushEnabled;

    /**
     * 채팅 알림 ON/OFF
     */
    private Boolean chatEnabled;

    /**
     * 마케팅 알림 ON/OFF
     */
    private Boolean marketingEnabled;

    /**
     * 마지막 변경 일시 (응답 시에만 포함)
     */
    private LocalDateTime updatedAt;
}

