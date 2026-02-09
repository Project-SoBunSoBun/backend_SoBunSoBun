package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.ChatMessage;
import com.sobunsobun.backend.domain.ChatRoom;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.repository.ChatMemberRepository;
import com.sobunsobun.backend.repository.ChatMessageRepository;
import com.sobunsobun.backend.repository.ChatRoomRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.sobunsobun.backend.support.exception.ErrorCode.*;

/**
 * 채팅 메시지 조회 서비스
 *
 * 기능:
 * - 메시지 목록 조회 (페이징)
 * - 커서 기반 페이징 (타임스탬프)
 * - 권한 검증
 * - ChatMessage → ChatMessageResponse 변환
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageQueryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    /**
     * 채팅방의 메시지 목록 조회 (최신순)
     *
     * REST API: GET /api/v1/chat/rooms/{id}/messages
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID (권한 검증용)
     * @param pageable 페이징 정보
     * @return 메시지 응답 페이지
     * @throws ChatException 채팅방 없음 또는 권한 없음
     */
    public Page<ChatMessageResponse> getMessages(Long roomId, Long userId, Pageable pageable) {
        // 권한 검증: 사용자가 멤버인지 확인
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.isMember(userId)) {
            log.warn("메시지 조회 권한 없음 - roomId: {}, userId: {}", roomId, userId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);

        // ChatMessage → ChatMessageResponse 변환
        List<ChatMessageResponse> responses = messages.getContent()
                .stream()
                .map(msg -> {
                    boolean readByMe = isMessageReadByUser(roomId, userId, msg.getId());
                    return toMessageResponse(msg, roomId, readByMe);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, messages.getTotalElements());
    }

    /**
     * 커서 기반 페이징: 특정 타임스탬프 이전 메시지 조회
     *
     * 사용 사례: 사용자가 위로 스크롤하여 이전 메시지 로드
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param cursor 커서 (타임스탬프)
     * @param pageable 페이징 정보
     * @return 메시지 응답 페이지
     */
    public Page<ChatMessageResponse> getMessagesBefore(
            Long roomId,
            Long userId,
            LocalDateTime cursor,
            Pageable pageable
    ) {
        // 권한 검증
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.isMember(userId)) {
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        Page<ChatMessage> messages = chatMessageRepository.findMessagesBeforeCursor(roomId, cursor, pageable);

        List<ChatMessageResponse> responses = messages.getContent()
                .stream()
                .map(msg -> {
                    boolean readByMe = isMessageReadByUser(roomId, userId, msg.getId());
                    return toMessageResponse(msg, roomId, readByMe);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, messages.getTotalElements());
    }

    /**
     * 사용자가 메시지를 읽었는지 확인
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param messageId 메시지 ID
     * @return 읽음 여부
     */
    private boolean isMessageReadByUser(Long roomId, Long userId, Long messageId) {
        var member = chatMemberRepository.findMember(roomId, userId).orElse(null);
        if (member == null || member.getLastReadMessageId() == null) {
            return false;
        }
        return member.getLastReadMessageId() >= messageId;
    }

    /**
     * ChatMessage를 ChatMessageResponse로 변환
     *
     * @param message 메시지 엔티티
     * @param roomId 채팅방 ID
     * @param readByMe 현재 사용자가 읽었는지 여부
     * @return 메시지 응답 DTO
     */
    private ChatMessageResponse toMessageResponse(ChatMessage message, Long roomId, boolean readByMe) {
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
}
