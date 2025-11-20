package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.enumClass.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateChatRoomRequest {
    private String title;
    private List<Long> memberIds;
    private ChatRoomType type;
    private Long postId;
}
