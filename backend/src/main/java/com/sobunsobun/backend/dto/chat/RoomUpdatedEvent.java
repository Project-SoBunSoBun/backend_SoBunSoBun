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
public class RoomUpdatedEvent {
    private Long roomId;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private Long lastMessageSenderId;
    private String lastMessageSenderName;
    private Long messageCount;
}
