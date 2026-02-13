package com.sobunsobun.backend.domain.chat;

import com.sobunsobun.backend.domain.BaseTimeEntity;
import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_room_id", columnList = "chat_room_id"),
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    // 메시지 타입 (TEXT, IMAGE, SYSTEM, INVITE_CARD, SETTLEMENT_CARD, ENTER, LEAVE 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageType type;

    // 메시지 내용
    @Column(columnDefinition = "LONGTEXT", name = "content")
    private String content;

    // 이미지 URL
    @Column(name = "image_url")
    private String imageUrl;

    // 카드 페이로드 (JSON)
    @Column(columnDefinition = "LONGTEXT", name = "card_payload")
    private String cardPayload;

    // 읽은 사람 수
    @Column(name = "read_count")
    private Integer readCount;

    @PrePersist
    protected void onCreate() {
        if (this.readCount == null) {
            this.readCount = 0;
        }
    }
}


