package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 초대장 생성 요청 DTO
 *
 * REST API: POST /api/v1/chat/invites
 *
 * {
 *   "privateChatRoomId": 123,
 *   "inviteeId": 456,
 *   "targetGroupPostId": 789
 * }
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatInviteRequest {

    /**
     * 개인 채팅방 ID (초대가 일어나는 1:1 채팅)
     */
    private Long privateChatRoomId;

    /**
     * 초대받을 사용자 ID
     */
    private Long inviteeId;

    /**
     * 목표 모임 ID (초대할 모임)
     */
    private Long targetGroupPostId;

    /**
     * 목표 채팅방 ID (초대할 단체 채팅방, 선택사항)
     */
    private Long targetChatRoomId;
}
