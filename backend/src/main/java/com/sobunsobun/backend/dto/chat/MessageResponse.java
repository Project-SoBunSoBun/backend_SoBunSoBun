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
public class MessageResponse {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String senderProfileImageUrl;
    private String type;
    private String content;
    private String imageUrl;
    private String cardPayload;
    private Integer readCount;
    private LocalDateTime createdAt;
    private Boolean readByMe;
}
