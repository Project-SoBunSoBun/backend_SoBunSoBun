package com.sobunsobun.backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 아이템 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationItemResponse {

    /**
     * 알림 ID
     */
    private Long id;

    /**
     * 알림 유형 (COMMENT, PARTICIPATION, SETTLE_UP, ANNOUNCE 등)
     */
    private String type;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 내용
     */
    private String message;

    /**
     * 관련 게시글 ID (있는 경우)
     */
    private Long postId;

    /**
     * 읽음 여부
     */
    private Boolean isRead;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;
}

