package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 항목 응답 DTO
 *
 * REST API: GET /api/v1/chat/rooms
 *
 * 채팅 목록 화면에서 사용:
 * {
 *   "roomId": 456,
 *   "name": "User1 & User2",
 *   "roomType": "PRIVATE",
 *   "lastMessagePreview": "마지막 메시지 미리보기...",
 *   "lastMessageSenderName": "User2",
 *   "unreadCount": 3,
 *   "lastMessageAt": "2025-01-27T10:30:00",
 *   "memberCount": 2,
 *   "profileImageUrl": "https://..." (개인채팅)
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomListItemResponse {

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 채팅방 이름
     */
    private String name;

    /**
     * 채팅방 타입 (PRIVATE, GROUP)
     */
    private String roomType;

    /**
     * 마지막 메시지 미리보기
     */
    private String lastMessagePreview;

    /**
     * 마지막 메시지 발송자 이름
     */
    private String lastMessageSenderName;

    /**
     * 읽지 않은 메시지 개수
     */
    private Long unreadCount;

    /**
     * 마지막 메시지 시간
     */
    private LocalDateTime lastMessageAt;

    /**
     * 활성 멤버 수
     */
    private Integer memberCount;

    /**
     * 상대방 프로필 이미지 (개인 채팅만)
     */
    private String profileImageUrl;

    /**
     * 상대방 ID (개인 채팅만)
     */
    private Long otherUserId;
}
