package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 읽음 처리 요청 DTO
 *
 * 클라이언트 → 서버: /app/chat/read
 *
 * iOS 사용 예:
 * {
 *   "roomId": 456,
 *   "lastReadMessageId": 789
 * }
 *
 * 읽음 처리 전략:
 * - 마지막으로 읽은 메시지 ID를 저장
 * - ChatMember.lastReadMessageId 업데이트
 * - unreadCount = MAX(messageId) - lastReadMessageId
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkAsReadRequest {

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 마지막으로 읽은 메시지 ID
     */
    private Long lastReadMessageId;
}
