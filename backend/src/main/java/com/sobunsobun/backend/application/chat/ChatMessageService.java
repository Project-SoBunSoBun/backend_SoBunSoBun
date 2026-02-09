package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.*;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.dto.chat.UnreadUpdatedEvent;
import com.sobunsobun.backend.repository.ChatMemberRepository;
import com.sobunsobun.backend.repository.ChatMessageRepository;
import com.sobunsobun.backend.repository.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.sobunsobun.backend.support.exception.ErrorCode.*;

/**
 * 채팅 메시지 관련 비즈니스 로직
 *
 * 기능:
 * - 메시지 저장 (TEXT, IMAGE, CARD)
 * - 읽음 처리 (lastReadMessageId 업데이트)
 * - unreadCount 계산
 * - 메시지 조회 (권한 검증)
 * - 채팅방 정보 업데이트 (lastMessageAt, lastMessagePreview)
 *
 * 트랜잭션 처리:
 * - 메시지 저장 + room 업데이트: 한 트랜잭션
 * - 읽음 처리: 별도 트랜잭션
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * 메시지 저장 및 채팅방 업데이트
     *
     * 흐름:
     * 1. 권한 검증 (사용자가 채팅방 멤버인지)
     * 2. 메시지 엔티티 생성
     * 3. ChatRoom.lastMessageAt, lastMessagePreview 업데이트
     * 4. DB 저장
     *
     * @param roomId 채팅방 ID
     * @param senderId 발송자 ID
     * @param type 메시지 타입
     * @param content 메시지 내용 (TEXT/SYSTEM)
     * @param imageUrl 이미지 URL (IMAGE)
     * @param cardPayload 카드 페이로드 (CARD)
     * @return 저장된 메시지 응답 DTO
     * @throws ChatException 권한 없음 또는 채팅방 없음
     */
    public ChatMessageResponse saveMessage(
            Long roomId,
            Long senderId,
            ChatMessageType type,
            String content,
            String imageUrl,
            String cardPayload
    ) {
        log.info("💾 [saveMessage] 시작 - roomId: {}, senderId: {}, type: {}",
                roomId, senderId, type);

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        log.info("📍 채팅방 조회 완료 - roomId: {}, name: {}, members: {}",
                roomId, chatRoom.getName(), chatRoom.getMembers().size());

        // 권한 검증
        validateChatRoomAccess(chatRoom, senderId);

        // ... existing code ...
        ChatMessage message;
        if (type == ChatMessageType.TEXT) {
            message = ChatMessage.createTextMessage(chatRoom,
                    userRepository.findById(senderId).get(), content);
        } else if (type == ChatMessageType.IMAGE) {
            message = ChatMessage.createImageMessage(chatRoom,
                    userRepository.findById(senderId).get(), imageUrl);
        } else if (type == ChatMessageType.INVITE_CARD) {
            message = ChatMessage.createInviteCardMessage(chatRoom,
                    userRepository.findById(senderId).get(), cardPayload);
        } else if (type == ChatMessageType.SETTLEMENT_CARD) {
            message = ChatMessage.createSettlementCardMessage(chatRoom,
                    userRepository.findById(senderId).get(), cardPayload);
        } else if (type == ChatMessageType.SYSTEM) {
            message = ChatMessage.createSystemMessage(chatRoom, content);
        } else {
            throw new IllegalArgumentException("지원하지 않는 메시지 타입: " + type);
        }

        // 메시지 저장
        ChatMessage savedMessage = chatMessageRepository.save(message);

        log.info("💾 메시지 DB 저장 완료 - messageId: {}, roomId: {}",
                savedMessage.getId(), roomId);

        // ChatRoom 정보 업데이트 (lastMessageAt, lastMessagePreview, lastMessageSenderId)
        chatRoom.setLastMessageAt(savedMessage.getCreatedAt());
        chatRoom.setLastMessageSenderId(senderId);

        // 미리보기 텍스트 생성
        String preview;
        if (type == ChatMessageType.IMAGE) {
            preview = "[이미지]";
        } else if (type == ChatMessageType.INVITE_CARD) {
            preview = "[초대장]";
        } else if (type == ChatMessageType.SETTLEMENT_CARD) {
            preview = "[정산서]";
        } else if (type == ChatMessageType.SYSTEM) {
            preview = content;
        } else {
            preview = content != null ? content.substring(0, Math.min(50, content.length())) : "";
        }
        chatRoom.setLastMessagePreview(preview);
        chatRoom.setMessageCount(chatRoom.getMessageCount() + 1);
        chatRoomRepository.save(chatRoom);

        log.info("✅ ChatRoom 업데이트 완료 - roomId: {}, messageCount: {}",
                roomId, chatRoom.getMessageCount());

        // 응답 DTO 생성
        ChatMessageResponse response = toMessageResponse(savedMessage, roomId, senderId);
        log.info("✅ 메시지 저장 완료 및 응답 생성 - messageId: {}", savedMessage.getId());

        return response;
    }

    /**
     * 메시지 읽음 처리
     *
     * 흐름:
     * 1. 채팅방 멤버 조회
     * 2. lastReadMessageId 업데이트
     * 3. unreadCount 재계산
     * 4. 읽음 이벤트 발행
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadMessageId 마지막으로 읽은 메시지 ID
     * @return 읽음 이벤트 (broadcast 용)
     * @throws ChatException 멤버 없음
     */
    public UnreadUpdatedEvent markAsRead(Long roomId, Long userId, Long lastReadMessageId) {
        // 멤버 조회
        ChatMember member = chatMemberRepository.findMember(roomId, userId)
                .orElseThrow(() -> new ChatException(CHAT_MEMBER_NOT_FOUND));

        // lastReadMessageId 업데이트
        member.setLastReadMessageId(lastReadMessageId);
        chatMemberRepository.save(member);

        // unreadCount 계산
        long unreadCount = chatMemberRepository.countUnreadMessages(roomId, userId);

        log.debug("읽음 처리 - roomId: {}, userId: {}, lastReadMessageId: {}, unreadCount: {}",
                roomId, userId, lastReadMessageId, unreadCount);

        return UnreadUpdatedEvent.builder()
                .roomId(roomId)
                .userId(userId)
                .lastReadMessageId(lastReadMessageId)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * 특정 채팅방의 unreadCount 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 미읽은 메시지 개수
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long roomId, Long userId) {
        return chatMemberRepository.countUnreadMessages(roomId, userId);
    }

    /**
     * ChatMessage를 ChatMessageResponse로 변환
     *
     * @param message 메시지 엔티티
     * @param roomId 채팅방 ID
     * @param currentUserId 현재 사용자 ID
     * @return 메시지 응답 DTO
     */
    private ChatMessageResponse toMessageResponse(ChatMessage message, Long roomId, Long currentUserId) {
        // 현재 사용자가 이 메시지를 읽었는지 확인
        ChatMember member = chatMemberRepository.findMember(roomId, currentUserId).orElse(null);
        boolean readByMe = member != null &&
                (member.getLastReadMessageId() != null &&
                 member.getLastReadMessageId() >= message.getId());

        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(roomId)
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSenderName())
                .senderProfileImageUrl(message.getSenderProfileImageUrl())
                .type(message.getType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .cardPayload(message.getCardPayload())
                .readCount(message.getReadCount())
                .createdAt(message.getCreatedAt())
                .readByMe(readByMe)
                .build();
    }

    /**
     * 메시지 엔티티를 응답 DTO로 변환 (unreadCount 없이)
     *
     * @param message 메시지 엔티티
     * @param roomId 채팅방 ID
     * @return 메시지 응답 DTO
     */
    @Transactional(readOnly = true)
    public ChatMessageResponse getMessageResponse(ChatMessage message, Long roomId) {
        // 현재 사용자 정보 없이 간단 변환
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(roomId)
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSenderName())
                .senderProfileImageUrl(message.getSenderProfileImageUrl())
                .type(message.getType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .cardPayload(message.getCardPayload())
                .readCount(message.getReadCount())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 채팅방의 메시지 조회
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesByRoomId(Long roomId, int limit) {
        log.info("📥 메시지 조회 시작 - roomId: {}, limit: {}", roomId, limit);

        try {
            // 최근 메시지 조회 (limit개, 내림차순)
            Page<ChatMessage> messagesPage = chatMessageRepository
                    .findByChatRoomIdOrderByCreatedAtDesc(
                            roomId,
                            Pageable.ofSize(limit)
                    );

            // 시간순 오름차순으로 정렬
            List<ChatMessage> messages = messagesPage.getContent().stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .collect(Collectors.toList());

            // DTO로 변환
            List<ChatMessageResponse> responses = messages.stream()
                    .map(message -> toMessageResponse(message, roomId, message.getSender().getId()))
                    .collect(Collectors.toList());

            log.info("✅ 메시지 조회 완료 - roomId: {}, count: {}", roomId, responses.size());
            return responses;

        } catch (Exception e) {
            log.error("❌ 메시지 조회 중 오류 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 채팅방 접근 권한 검증
     *
     * @param chatRoom 채팅방
     * @param userId 사용자 ID
     * @throws ChatException 권한 없음
     */
    private void validateChatRoomAccess(ChatRoom chatRoom, Long userId) {
        boolean isMember = chatRoom.isMember(userId);
        log.info("✅ 권한 검증 - roomId: {}, userId: {}, isMember: {}",
                chatRoom.getId(), userId, isMember);

        if (!isMember) {
            log.warn("❌ 채팅방 접근 권한 없음 - roomId: {}, userId: {}",
                    chatRoom.getId(), userId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }
    }
}

