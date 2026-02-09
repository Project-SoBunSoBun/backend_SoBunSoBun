package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 * 텍스트, 이미지, 초대장, 정산서, 시스템 메시지를 모두 지원
 *
 * 설계:
 * - chatRoom: 채팅방
 * - sender: 메시지 발송자
 * - type: 메시지 타입 (TEXT, IMAGE, INVITE_CARD, SETTLEMENT_CARD, SYSTEM)
 * - content: 메시지 본문
 * - imageUrl: 이미지 메시지인 경우 이미지 URL
 * - cardPayload: 카드 타입의 경우 JSON 페이로드 (inviteId, settleUpId 등)
 * - readCount: 읽은 멤버 수 (캐시)
 * - createdAt: 생성 시간 (정렬용)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_room_created", columnList = "chat_room_id,created_at DESC"),
        @Index(name = "idx_chat_room_id", columnList = "chat_room_id"),
        @Index(name = "idx_sender_id", columnList = "sender_id")
})
public class ChatMessage {
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
     * 메시지 발송자
     * SYSTEM 메시지인 경우 null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    /**
     * 메시지 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageType type;

    /**
     * 메시지 내용
     * TEXT/SYSTEM 메시지의 경우 필수
     * 최대 3000자
     */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 이미지 메시지인 경우 이미지 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 카드 타입의 경우 JSON 페이로드
     * INVITE_CARD: {"inviteId": 123, "invitedUserId": 456, ...}
     * SETTLEMENT_CARD: {"settleUpId": 789, "status": "COMPLETED", ...}
     */
    @Column(name = "card_payload", columnDefinition = "LONGTEXT")
    private String cardPayload;

    /**
     * 읽은 멤버 수 (캐시)
     * 성능 최적화용 - ChatMember.lastReadMessageId와 일관성 유지 필요
     */
    @Column(nullable = false)
    @Builder.Default
    private Long readCount = 0L;

    /**
     * 메시지 생성 시간
     * 정렬 및 페이징용 (커서 기반)
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 발송자 이름 (조회 성능 최적화용 캐시)
     */
    @Column(name = "sender_name", length = 40)
    private String senderName;

    /**
     * 발송자 프로필 이미지 URL (조회 성능 최적화용 캐시)
     */
    @Column(name = "sender_profile_image_url", length = 500)
    private String senderProfileImageUrl;

    /**
     * 시스템 메시지 생성 헬퍼 (참여, 퇴장 등)
     */
    public static ChatMessage createSystemMessage(ChatRoom chatRoom, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .readCount(0L)
                .build();
    }

    /**
     * 텍스트 메시지 생성 헬퍼
     */
    public static ChatMessage createTextMessage(ChatRoom chatRoom, User sender, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .type(ChatMessageType.TEXT)
                .content(content)
                .senderName(sender.getNickname())
                .senderProfileImageUrl(sender.getProfileImageUrl())
                .readCount(0L)
                .build();
    }

    /**
     * 이미지 메시지 생성 헬퍼
     */
    public static ChatMessage createImageMessage(ChatRoom chatRoom, User sender, String imageUrl) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .type(ChatMessageType.IMAGE)
                .imageUrl(imageUrl)
                .senderName(sender.getNickname())
                .senderProfileImageUrl(sender.getProfileImageUrl())
                .readCount(0L)
                .build();
    }

    /**
     * 초대장 카드 메시지 생성 헬퍼
     */
    public static ChatMessage createInviteCardMessage(ChatRoom chatRoom, User sender, String cardPayload) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .type(ChatMessageType.INVITE_CARD)
                .cardPayload(cardPayload)
                .senderName(sender.getNickname())
                .senderProfileImageUrl(sender.getProfileImageUrl())
                .readCount(0L)
                .build();
    }

    /**
     * 정산 카드 메시지 생성 헬퍼
     */
    public static ChatMessage createSettlementCardMessage(ChatRoom chatRoom, User sender, String cardPayload) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .type(ChatMessageType.SETTLEMENT_CARD)
                .cardPayload(cardPayload)
                .senderName(sender.getNickname())
                .senderProfileImageUrl(sender.getProfileImageUrl())
                .readCount(0L)
                .build();
    }
}
