package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.*;
import com.sobunsobun.backend.dto.chat.MessageResponse;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * 메시지 저장
     *
     * 1. 채팅방 멤버 권한 확인
     * 2. 메시지 DB 저장
     * 3. 채팅방 마지막 메시지 정보 업데이트
     * 4. DTO 반환
     */
    public MessageResponse saveMessage(
            Long roomId,
            Long senderId,
            ChatMessageType type,
            String content,
            String imageUrl,
            String cardPayload
    ) {
        // 1. 채팅방 조회 및 권한 검증
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

        Hibernate.initialize(chatRoom.getMembers());

        if (!chatRoom.isMember(senderId)) {
            throw new RuntimeException("User is not a member of this chat room");
        }

        // 2. 사용자 조회
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found: " + senderId));

        // 3. 메시지 생성 및 저장
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .type(type)
                .content(content)
                .imageUrl(imageUrl)
                .cardPayload(cardPayload)
                .readCount(0)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 4. 채팅방 메타데이터 업데이트
        chatRoom.setLastMessageAt(savedMessage.getCreatedAt());
        chatRoom.setLastMessagePreview(truncateContent(content));
        chatRoom.setLastMessageSenderId(senderId);
        chatRoomRepository.save(chatRoom);

        log.info("✅ 메시지 저장 - roomId: {}, messageId: {}", roomId, savedMessage.getId());

        return toMessageResponse(savedMessage, senderId);
    }

    /**
     * 메시지 읽음 처리
     *
     * 마지막으로 읽은 메시지 ID를 저장
     */
    public void markAsRead(Long roomId, Long userId, Long lastReadMessageId) {
        ChatMember member = chatMemberRepository.findMember(roomId, userId)
                .orElseThrow(() -> new RuntimeException("Chat member not found"));

        // 이전 값이 더 크면 업데이트 안 함 (더 최신을 읽었을 경우)
        if (member.getLastReadMessageId() != null && member.getLastReadMessageId() >= lastReadMessageId) {
            return;
        }

        member.setLastReadMessageId(lastReadMessageId);
        chatMemberRepository.save(member);

        log.info("📖 읽음 처리 - roomId: {}, userId: {}, lastReadMessageId: {}",
                roomId, userId, lastReadMessageId);
    }

    /**
     * DTO 변환
     */
    private MessageResponse toMessageResponse(ChatMessage message, Long requesterId) {
        ChatMember member = chatMemberRepository.findMember(
                message.getChatRoom().getId(),
                requesterId
        ).orElse(null);

        boolean readByMe = member != null &&
                member.getLastReadMessageId() != null &&
                member.getLastReadMessageId() >= message.getId();

        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getNickname())
                .senderProfileImageUrl(message.getSender().getProfileImageUrl())
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
     * 미리보기 텍스트 생성 (너무 길면 잘라냄)
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
