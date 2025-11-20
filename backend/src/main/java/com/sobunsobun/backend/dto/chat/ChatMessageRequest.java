package com.sobunsobun.backend.dto.chat;

import com.sobunsobun.backend.enumClass.ChatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageRequest {
    @NotNull
    private ChatType type;
    @NotNull
    private Long senderId;
    @NotNull
    private Long roomId;

    private String content;

    private String sendAt;
}