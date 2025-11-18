package com.sobunsobun.backend.entity.chat;

import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;

@Table(name = "chat_member")
@Entity
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private User member;
}

