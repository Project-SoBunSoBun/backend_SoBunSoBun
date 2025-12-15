package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMemberResponse {
    Long userId;

    String nickname;

    String profileImageUrl;

    ChatMemberRole memberRole;

    ChatMemberStatus status;

    Instant createdAt;

    /**
     * ChatMember 엔티티를 ChatMemberResponse로 변환
     */
    public static ChatMemberResponse from(ChatMember chatMember) {
        return ChatMemberResponse.builder()
                .userId(chatMember.getMember().getId())
                .nickname(chatMember.getMember().getNickname())
                .profileImageUrl(chatMember.getMember().getProfileImageUrl())
                .memberRole(chatMember.getRole())
                .status(chatMember.getStatus())
                .createdAt(chatMember.getCreatedAt())
                .build();
    }
}
