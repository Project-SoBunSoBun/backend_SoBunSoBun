package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초대 수락 요청 DTO
 *
 * REST API: PUT /api/v1/chat/invites/{id}/accept
 * 또는 WebSocket: /app/chat/invite/accept
 *
 * {
 *   "targetGroupChatRoomId": 789
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptChatInviteRequest {

    /**
     * 초대받은 단체 채팅방 ID
     * 수락 시 이 채팅방에 멤버로 추가됨
     */
    private Long targetGroupChatRoomId;
}
