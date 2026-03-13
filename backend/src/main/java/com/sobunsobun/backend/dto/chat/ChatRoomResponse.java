package com.sobunsobun.backend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoomResponse {
    private Long id;
    private String name;
    private String roomType;
    private Long ownerId;
    private Integer memberCount;
    @JsonProperty("unReadCount")
    private Long unreadCount;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
}
