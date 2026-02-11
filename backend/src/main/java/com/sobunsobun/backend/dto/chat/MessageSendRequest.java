package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.domain.chat.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageSendRequest {
    private Long roomId;
    private ChatMessageType type;
    private String content;
    private String imageUrl;
    private String cardPayload;
}
