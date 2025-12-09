package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Setter
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.OPEN;

    @Setter
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Setter
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ChatRoom(String title, ChatRoomType type, Long ownerId, Long postId, String imageUrl) {
        this.title = title;
        this.type = type;
        this.ownerId = ownerId;
        this.postId = postId;
        this.imageUrl = imageUrl;
        this.status = ChatRoomStatus.OPEN;
    }
}
