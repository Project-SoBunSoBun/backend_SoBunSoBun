package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 목록 실시간 업데이트 WebSocket 이벤트 페이로드
 *
 * 구독 경로: /sub/users/{userId}/chat-rooms
 *
 * 예시:
 * {
 *   "type": "CHAT_LIST_UPDATE",
 *   "roomId": 42,
 *   "roomName": "홍길동",
 *   "profileImageUrl": "https://...",
 *   "lastMessage": "안녕하세요",
 *   "lastMessageAt": "2026-02-27T12:34:56+09:00",
 *   "unReadCount": 3,
 *   "roomType": "ONE_TO_ONE"
 * }
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatListUpdateNotification {

    /** 이벤트 타입 (항상 "CHAT_LIST_UPDATE") */
    private String type;

    /** 채팅방 ID */
    private Long roomId;

    /**
     * 채팅방 표시 이름
     * - ONE_TO_ONE: 상대방 닉네임
     * - GROUP: 방 이름
     */
    private String roomName;

    /**
     * 프로필 이미지 URL
     * - ONE_TO_ONE: 상대방 프로필 이미지
     * - GROUP: null (GroupPost 이미지 미지원)
     */
    private String profileImageUrl;

    /** 마지막 메시지 정보 */
    private LastMessageDto lastMessage;

    /**
     * 안 읽은 메시지 수
     * - Redis Hash에서 조회 (Cache Miss 시 DB 폴백)
     * - 현재 해당 방에 접속 중인 유저: 0
     */
    @JsonProperty("unReadCount")
    private Integer unreadCount;

    /** 채팅방 타입: "ONE_TO_ONE" | "GROUP" */
    private String roomType;
}
