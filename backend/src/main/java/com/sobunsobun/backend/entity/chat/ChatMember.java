package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Table(name = "chat_member")
@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private User member;

    @Setter
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChatMemberStatus status = ChatMemberStatus.ACTIVE;

    @Setter
    @Column(name = "left_at")
    private Instant leftAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

