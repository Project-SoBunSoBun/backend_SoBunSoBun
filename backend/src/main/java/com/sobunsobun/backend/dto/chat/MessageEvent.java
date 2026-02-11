package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageEvent {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String type;
    private String content;
    private String cardPayload;
    private LocalDateTime createdAt;
}
