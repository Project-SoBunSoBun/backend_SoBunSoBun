package com.sobunsobun.backend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KickMemberResponse {

    private Long roomId;
    private Long kickedUserId;
    private String kickedUserNickname;
    private Integer remainingMembers;
}
