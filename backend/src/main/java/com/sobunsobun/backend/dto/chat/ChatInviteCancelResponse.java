package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.chat.ChatInvite;
import com.sobunsobun.backend.domain.chat.ChatInviteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatInviteCancelResponse {

    private Long inviteId;
    private ChatInviteStatus status;
    private LocalDateTime updatedAt;

    public static ChatInviteCancelResponse from(ChatInvite invite) {
        return ChatInviteCancelResponse.builder()
                .inviteId(invite.getId())
                .status(invite.getStatus())
                .updatedAt(invite.getUpdatedAt())
                .build();
    }
}
