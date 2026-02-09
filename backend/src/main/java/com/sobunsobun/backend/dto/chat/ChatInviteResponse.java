package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대장 응답 DTO
 *
 * REST API: GET /api/v1/chat/invites
 * WebSocket: INVITE_CARD 메시지 cardPayload
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatInviteResponse {

    /**
     * 초대장 ID
     */
    private Long inviteId;

    /**
     * 초대 채팅방 ID (개인 채팅)
     */
    private Long chatRoomId;

    /**
     * 초대한 사용자 ID
     */
    private Long inviterId;

    /**
     * 초대한 사용자 이름
     */
    private String inviterName;

    /**
     * 초대한 사용자 프로필
     */
    private String inviterProfileImageUrl;

    /**
     * 초대받은 사용자 ID
     */
    private Long inviteeId;

    /**
     * 초대 상태 (PENDING, ACCEPTED, DECLINED, EXPIRED)
     */
    private String status;

    /**
     * 초대 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * 초대 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 목표 모임 ID (초대할 단체 채팅의 모임)
     */
    private Long targetGroupPostId;

    /**
     * 목표 채팅방 ID (초대할 단체 채팅방)
     */
    private Long targetChatRoomId;
}
