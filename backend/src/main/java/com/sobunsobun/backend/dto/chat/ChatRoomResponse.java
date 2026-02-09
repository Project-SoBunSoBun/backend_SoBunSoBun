package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 상세 정보 응답 DTO
 *
 * REST API: GET /api/v1/chat/rooms/{id}
 * 또는 WebSocket 입장 후 채팅방 정보 조회
 *
 * 채팅방 화면에서 사용:
 * {
 *   "roomId": 456,
 *   "name": "User1 & User2",
 *   "roomType": "PRIVATE",
 *   "memberCount": 2,
 *   "members": [
 *     {
 *       "userId": 789,
 *       "nickname": "User1",
 *       "profileImageUrl": "https://..."
 *     }
 *   ],
 *   "isOwner": true,
 *   "createdAt": "2025-01-27T10:30:00"
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomResponse {

    /**
     * 채팅방 ID
     */
    private Long roomId;

    /**
     * 채팅방 이름
     */
    private String name;

    /**
     * 채팅방 타입
     */
    private String roomType;

    /**
     * 활성 멤버 수
     */
    private Integer memberCount;

    /**
     * 멤버 목록
     */
    private List<ChatRoomMemberInfo> members;

    /**
     * 현재 사용자가 방장인지 여부
     */
    private Boolean isOwner;

    /**
     * 채팅방 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 채팅방 멤버 정보 (간단)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRoomMemberInfo {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
    }
}
