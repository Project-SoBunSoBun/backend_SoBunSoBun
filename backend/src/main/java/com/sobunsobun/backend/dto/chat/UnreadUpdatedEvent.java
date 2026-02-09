package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음 처리 이벤트 응답 DTO
 *
 * 서버 → 클라이언트: /user/{userId}/queue/private 또는 /topic/rooms/{roomId}/read
 *
 * iOS에서 다른 사용자가 메시지를 읽었을 때 UI 업데이트:
 * {
 *   "roomId": 456,
 *   "userId": 789,
 *   "lastReadMessageId": 999,
 *   "unreadCount": 0
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadUpdatedEvent {

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 읽음 처리한 사용자 ID
     */
    private Long userId;

    /**
     * 마지막으로 읽은 메시지 ID
     */
    private Long lastReadMessageId;

    /**
     * 현재 미읽은 메시지 개수
     */
    private Long unreadCount;
}
