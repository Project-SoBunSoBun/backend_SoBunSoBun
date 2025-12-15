package com.sobunsobun.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagePage {
    private List<ChatMessageResponse> messages;
    private Long nextCursorMillis;
    private boolean hasNext;
}
