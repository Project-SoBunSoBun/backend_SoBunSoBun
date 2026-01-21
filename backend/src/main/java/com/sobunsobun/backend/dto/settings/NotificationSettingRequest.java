package com.sobunsobun.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 변경 요청 DTO
 * 모든 필드는 선택적이며, 제공된 필드만 업데이트
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingRequest {

    /**
     * 전체 푸시 알림 ON/OFF
     * - false인 경우 다른 알림 설정과 무관하게 모든 푸시 발송 안 함
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
}

