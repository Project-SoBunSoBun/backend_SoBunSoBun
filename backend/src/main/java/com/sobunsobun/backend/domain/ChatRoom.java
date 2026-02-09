package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 * 개인 채팅(1:1)과 단체 채팅(그룹) 모두 지원
 *
 * 설계:
 * - name: 개인채팅의 경우 "User1 & User2", 단체채팅의 경우 모임명
 * - roomType: PRIVATE(개인) 또는 GROUP(단체)
 * - owner: 채팅방 생성자 (방장)
 * - groupPostId: 단체채팅인 경우 해당 모임(GroupPost) ID
 * - lastMessageAt: 마지막 메시지 시간 (목록 정렬용)
 * - lastMessagePreview: 마지막 메시지 미리보기 (목록 표시용)
 * - members: 채팅방 멤버 (ChatMember)
 * - messages: 채팅 메시지 (ChatMessage)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_room")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 채팅방 이름
     * 개인채팅: "User1 & User2"
     * 단체채팅: 모임 이름
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 채팅방 타입 (PRIVATE, GROUP)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType roomType;

    /**
     * 채팅방 생성자 (방장)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * 단체채팅인 경우 연결된 모임(GroupPost) ID
     * 개인채팅의 경우 null
     */
    @Column(name = "group_post_id")
    private Long groupPostId;

    /**
     * 마지막 메시지 시간 (목록 정렬용)
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 마지막 메시지 미리보기 (채팅 목록 표시용)
     */
    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    /**
     * 마지막 메시지를 보낸 사용자 ID (목록에서 누가 보냈는지 표시용)
     */
    @Column(name = "last_message_sender_id")
    private Long lastMessageSenderId;

    /**
     * 메시지 카운트 (성능 최적화용 캐시)
     */
    @Column(nullable = false)
    @Builder.Default
    private Long messageCount = 0L;

    /**
     * 채팅방 생성 시간
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 채팅방 수정 시간
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 채팅방 멤버 목록
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();

    /**
     * 채팅 메시지 목록
     */
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 해당 채팅방에서 사용자가 읽은 마지막 메시지 ID 조회
     * 성능 최적화를 위해 ChatMember의 lastReadMessageId를 사용
     */
    public Long getLastReadMessageIdByUser(Long userId) {
        return members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .map(ChatMember::getLastReadMessageId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 사용자가 채팅방 멤버인지 확인
     */
    public boolean isMember(Long userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.getStatus() == ChatMemberStatus.ACTIVE);
    }

    /**
     * 활성 멤버 수 조회
     */
    public int getActiveMemberCount() {
        return (int) members.stream()
                .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                .count();
    }

    /**
     * 방장 여부 확인
     */
    public boolean isOwner(Long userId) {
        return owner.getId().equals(userId);
    }
}
