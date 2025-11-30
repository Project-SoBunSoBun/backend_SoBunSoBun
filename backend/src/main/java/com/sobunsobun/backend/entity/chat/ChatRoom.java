package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.enumClass.ChatRoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "chat_room")
@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "post_id")
    private Long postId;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updateAt;

    public ChatRoom(String title, ChatRoomType type, Long ownerId, Long postId) {
        this.title = title;
        this.type = type;
        this.ownerId = ownerId;
        this.postId = postId;
    }
}
