package com.sobunsobun.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatInviteRequest {

    @NotNull(message = "초대할 사용자 ID는 필수입니다.")
    private Long inviteeId;
}
