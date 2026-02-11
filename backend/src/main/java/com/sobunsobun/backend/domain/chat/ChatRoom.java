package com.sobunsobun.backend.domain.chat;

import com.sobunsobun.backend.domain.BaseTimeEntity;
import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "chat_room", indexes = {
        @Index(name = "idx_room_type", columnList = "room_type"),
        @Index(name = "idx_group_post_id", columnList = "group_post_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // 단체 채팅방: groupPostId로 모임 연결
    @Column(name = "group_post_id")
    private Long groupPostId;

    // 채팅방 정보 (마지막 메시지, 메시지 개수)
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 500)
    private String lastMessagePreview;

    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    @Column(name = "message_count")
    private Long messageCount;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ChatMember> members = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (this.messageCount == null) {
            this.messageCount = 0L;
        }
        if (this.members == null) {
            this.members = new HashSet<>();
        }
    }

    // 편의 메서드
    public boolean isMember(Long userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.getStatus() == ChatMemberStatus.ACTIVE);
    }

    public boolean isOwner(Long userId) {
        return owner.getId().equals(userId);
    }

    public ChatMember addMember(User user) {
        ChatMember member = ChatMember.builder()
                .chatRoom(this)
                .user(user)
                .status(ChatMemberStatus.ACTIVE)
                .build();
        members.add(member);
        return member;
    }

    public void removeMember(Long userId) {
        members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .forEach(m -> m.setStatus(ChatMemberStatus.LEFT));
    }
}
