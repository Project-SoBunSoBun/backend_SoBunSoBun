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
public class ReadEvent {
    private Long roomId;
    private Long userId;
    private Long lastReadMessageId;
    private Long unreadCount;
    private LocalDateTime timestamp;
}
