package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InviteCardPayload {
    private Long inviteId;
    private Long inviterId;
    private String inviterName;
    private String inviterProfileUrl;
    private Long groupPostId;
    private String groupPostTitle;
    private String expiresAt;
}
