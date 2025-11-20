package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;
import lombok.Builder;

@Table(name = "chat_member")
@Entity
@Builder
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private User member;
}

