package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ChatRoomResponse {
    Long roomId;
    Long postId;
    String title;
    ChatRoomType chatRoomType;
    List<ChatMember> chatMembers;
}
