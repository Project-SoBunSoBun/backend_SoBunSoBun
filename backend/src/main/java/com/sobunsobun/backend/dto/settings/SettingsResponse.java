package com.sobunsobun.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전체 설정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsResponse {

    /**
     * 지역 설정
     */
    private RegionSettingResponse region;

    /**
     * 알림 설정
     */
    private NotificationSettingResponse notification;

    /**
     * 앱 정보
     */
    private AppInfoResponse app;
}

