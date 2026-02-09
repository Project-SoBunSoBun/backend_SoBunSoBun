package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 초대장 엔티티
 * 개인 채팅에서 방장이 초대를 보낼 때 사용
 *
 * 설계:
 * - chatRoom: 초대할 채팅방
 * - inviter: 초대를 보낸 사용자 (채팅방 방장)
 * - invitee: 초대받은 사용자
 * - status: 초대 상태 (PENDING, ACCEPTED, DECLINED, EXPIRED)
 * - expiresAt: 초대 만료 시간 (7일)
 * - acceptedAt: 초대 수락 시간
 *
 * 초대 흐름:
 * 1. 방장이 초대장 생성 (PENDING)
 * 2. ChatMessage(INVITE_CARD)로 브로드캐스트
 * 3. 초대받은 사용자가 수락 (ACCEPTED)
 * 4. ChatMember 추가, 단체 채팅방에 조인
 * 5. 시스템 메시지 "User joined" 전송
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_invite", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "invitee_id"})
})
public class ChatInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 초대할 채팅방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 초대를 보낸 사용자 (보통 방장)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    /**
     * 초대받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    /**
     * 초대 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatInviteStatus status = ChatInviteStatus.PENDING;

    /**
     * 초대 만료 시간 (기본 7일)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 초대 수락 시간
     */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /**
     * 초대 생성 시간
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 초대 수정 시간 (상태 변경 시)
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 초대 만료 확인
     */
    public boolean isExpired() {
        return status == ChatInviteStatus.EXPIRED ||
               (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    /**
     * 초대 수락
     */
    public void accept() {
        this.status = ChatInviteStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    /**
     * 초대 거절
     */
    public void decline() {
        this.status = ChatInviteStatus.DECLINED;
    }
}
