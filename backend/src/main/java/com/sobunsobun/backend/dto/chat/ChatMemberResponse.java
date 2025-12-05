package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.enumClass.ChatMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMemberResponse {
    Long userId;

    String nickname;

    String profileImageUrl;

    ChatMemberRole memberRole;
}
