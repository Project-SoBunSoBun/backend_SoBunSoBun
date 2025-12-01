package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.entity.chat.ChatMessage;
import com.sobunsobun.backend.enumClass.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.sobunsobun.backend.support.util.TimeUtil.normalizeToKstIso;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {

    private String id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private ChatType type;
    private String content;
    private String sentAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .type(message.getType())
                .content(message.getContent())
                .sentAt(normalizeToKstIso(message.getCreatedAt()))
                .build();
    }

    public static ChatMessageResponse from(ChatMessage message, User sender) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getNickname() : null)
                .type(message.getType())
                .content(message.getContent())
                .sentAt(normalizeToKstIso(message.getCreatedAt()))
                .build();
    }
}
