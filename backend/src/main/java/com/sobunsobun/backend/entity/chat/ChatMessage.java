package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.dto.chat.ChatMessageRequest;
import com.sobunsobun.backend.enumClass.ChatType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_message")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private Long roomId;

    @Indexed
    private Long senderId;

    private ChatType type;

    private String content;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    public ChatMessage(Long roomId, Long senderId, ChatMessageRequest request) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.type = request.getType();
        this.content = request.getContent();
    }
}
