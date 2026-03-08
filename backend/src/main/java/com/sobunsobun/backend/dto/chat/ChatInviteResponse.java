package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.chat.ChatInvite;
import com.sobunsobun.backend.domain.chat.ChatInviteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatInviteResponse {

    private Long inviteId;
    private Long chatRoomId;
    private String roomName;
    private Long inviterId;
    private String inviterName;
    private String inviterProfileUrl;
    private Long inviteeId;
    private ChatInviteStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static ChatInviteResponse from(ChatInvite invite) {
        return ChatInviteResponse.builder()
                .inviteId(invite.getId())
                .chatRoomId(invite.getChatRoom().getId())
                .roomName(invite.getChatRoom().getName())
                .inviterId(invite.getInviter().getId())
                .inviterName(invite.getInviter().getNickname())
                .inviterProfileUrl(invite.getInviter().getProfileImageUrl())
                .inviteeId(invite.getInvitee().getId())
                .status(invite.getStatus())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .build();
    }
}
