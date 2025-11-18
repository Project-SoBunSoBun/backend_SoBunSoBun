package com.sobunsobun.backend.dto.websocket;

import com.sobunsobun.backend.enumClass.ChatType;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageRequest {
    private ChatType type;

    private Long senderId;

    private Long roomId;

    private String text;

    private String sendAt;
}