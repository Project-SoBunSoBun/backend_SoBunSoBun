package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ChatRoomResponse {
    Long roomId;
    Long postId;
    String title;
    String imageUrl;
    ChatRoomType chatRoomType;
    List<ChatMemberResponse> chatMembers;

    @Builder
    public ChatRoomResponse(Long roomId, Long postId, String title, String imageUrl, ChatRoomType chatRoomType, List<ChatMember> chatMembers) {
        this.roomId = roomId;
        this.postId = postId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.chatRoomType = chatRoomType;
        this.chatMembers = new ArrayList<>();

        chatMembers.forEach(chatMember -> {
            User user = chatMember.getMember();

            this.chatMembers.add(
                    ChatMemberResponse.builder()
                            .userId(user.getId())
                            .nickname(user.getNickname())
                            .profileImageUrl(user.getProfileImageUrl())
                            .memberRole(chatMember.getRole())
                            .build()
            );
        });
    }
}
