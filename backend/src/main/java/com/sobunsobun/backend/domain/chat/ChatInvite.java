package com.sobunsobun.backend.domain.chat;

import com.sobunsobun.backend.domain.BaseTimeEntity;
import com.sobunsobun.backend.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_invite", indexes = {
        @Index(name = "idx_invitee_id", columnList = "invitee_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatInvite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 초대를 보낸 채팅방 (개인 1:1 채팅방)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // 초대를 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    // 초대받은 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    // 초대 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatInviteStatus status = ChatInviteStatus.PENDING;

    // 초대 만료 시간
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // 초대가 가리키는 모임 ID
    @Column(name = "target_group_post_id")
    private Long targetGroupPostId;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = ChatInviteStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ChatInviteStatus.REJECTED;
    }
}
