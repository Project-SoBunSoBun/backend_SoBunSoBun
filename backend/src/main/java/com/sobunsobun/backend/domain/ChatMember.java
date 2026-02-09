package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 엔티티
 * ChatRoom과 User의 다대다 관계를 나타냄
 *
 * 설계:
 * - chatRoom: 채팅방
 * - user: 사용자
 * - status: 멤버 상태 (ACTIVE, LEFT, INVITED)
 * - lastReadMessageId: 마지막으로 읽은 메시지 ID (읽음 처리용)
 * - joinedAt: 채팅방 참여 시간
 *
 * 읽음 처리 전략:
 * - lastReadMessageId를 저장하여 성능 최적화
 * - unreadCount = 최신 메시지 ID - lastReadMessageId
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})
})
public class ChatMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    /**
     * 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 멤버 상태
     * ACTIVE: 활성 멤버
     * LEFT: 퇴장 (소프트 삭제)
     * INVITED: 초대 상태 (단체채팅에서 초대 수락 전)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatMemberStatus status = ChatMemberStatus.ACTIVE;

    /**
     * 마지막으로 읽은 메시지 ID
     * null이면 읽은 메시지 없음
     * 읽음 처리 시 업데이트됨
     */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    /**
     * 참여 시간
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 읽지 않은 메시지 개수 계산 (트랜지언트)
     * 실제 계산은 Service에서 수행
     */
    @Transient
    private Long unreadCount;

    /**
     * 읽지 않은 메시지 개수 업데이트
     */
    public void setUnreadCount(Long count) {
        this.unreadCount = count;
    }
}
