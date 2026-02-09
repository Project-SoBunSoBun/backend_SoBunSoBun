package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.ChatMessageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 DTO
 *
 * 서버 → 클라이언트: /topic/rooms/{roomId}
 *
 * iOS 클라이언트에서 사용:
 * {
 *   "id": 123,
 *   "roomId": 456,
 *   "senderId": 789,
 *   "senderName": "User Name",
 *   "senderProfileImageUrl": "https://...",
 *   "type": "TEXT",
 *   "content": "Hello",
 *   "imageUrl": null,
 *   "cardPayload": null,
 *   "readCount": 5,
 *   "createdAt": "2025-01-27T10:30:00",
 *   "readByMe": false
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {

    /**
     * 메시지 ID
     */
    private Long id;

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 발송자 ID (SYSTEM 메시지인 경우 null)
     */
    private Long senderId;

    /**
     * 발송자 이름
     */
    private String senderName;

    /**
     * 발송자 프로필 이미지 URL
     */
    private String senderProfileImageUrl;

    /**
     * 메시지 타입
     */
    private ChatMessageType type;

    /**
     * 메시지 내용 (TEXT/SYSTEM)
     */
    private String content;

    /**
     * 이미지 URL (IMAGE)
     */
    private String imageUrl;

    /**
     * 카드 페이로드 (JSON 문자열)
     */
    private String cardPayload;

    /**
     * 읽은 멤버 수
     */
    private Long readCount;

    /**
     * 메시지 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 현재 사용자가 이 메시지를 읽었는지 여부
     * 클라이언트에서 UI 처리용
     */
    @Builder.Default
    private Boolean readByMe = false;
}
