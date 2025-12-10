package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.enumClass.ChatMemberRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberRequest {
    Long memberId;
    ChatMemberRole chatMemberRole;
}
