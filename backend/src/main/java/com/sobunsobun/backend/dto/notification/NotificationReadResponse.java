package com.sobunsobun.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 읽음 처리 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationReadResponse {

    /**
     * 알림 ID
     */
    private Long id;

    /**
     * 읽음 여부
     */
    private Boolean isRead;

    /**
     * 읽은 일시
     */
    private LocalDateTime readAt;
}

