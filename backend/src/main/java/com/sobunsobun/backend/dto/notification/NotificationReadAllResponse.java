package com.sobunsobun.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전체 알림 읽음 처리 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationReadAllResponse {

    /**
     * 읽음 처리된 알림 수
     */
    private Integer updatedCount;

    /**
     * 읽은 일시
     */
    private LocalDateTime readAt;
}

