package com.sobunsobun.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽지 않은 알림 수 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountResponse {

    /**
     * 읽지 않은 알림 수
     */
    private Integer unreadCount;
}

