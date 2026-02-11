package com.sobunsobun.backend.domain.chat;

import com.sobunsobun.backend.domain.BaseTimeEntity;
import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_member", indexes = {
        @Index(name = "idx_chat_room_user", columnList = "chat_room_id,user_id"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatMemberStatus status = ChatMemberStatus.ACTIVE;

    // 읽음 처리: 마지막으로 읽은 메시지 ID
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}
