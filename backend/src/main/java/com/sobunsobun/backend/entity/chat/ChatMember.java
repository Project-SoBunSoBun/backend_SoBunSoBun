package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}

