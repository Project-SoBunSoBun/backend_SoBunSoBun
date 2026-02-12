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
    @Transactional(rollbackFor = Exception.class)
    public MessageResponse saveMessage(
            Long roomId,
            Long senderId,
            ChatMessageType type,
            String content,
            String imageUrl,
            String cardPayload
    ) {
        try {
            log.info("📝 [메시지 저장 시작] roomId: {}, senderId: {}, type: {}, contentLength: {}",
                    roomId, senderId, type, content != null ? content.length() : 0);

            // 1. 채팅방 조회 및 권한 검증
            log.debug("🔍 [단계1] 채팅방 조회 중... roomId: {}", roomId);
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> {
                        log.error("❌ [단계1 실패] 채팅방을 찾을 수 없음: roomId={}", roomId);
                        return new RuntimeException("Chat room not found: " + roomId);
                    });
            log.debug("✅ [단계1 성공] 채팅방 조회됨: roomId={}, roomName={}", chatRoom.getId(), chatRoom.getName());

            int memberCount = chatRoom.getMembers() != null ? chatRoom.getMembers().size() : 0;
            log.debug("✅ [단계1] 멤버 정보 로드됨: memberCount={}, members={}",
                    memberCount,
                    chatRoom.getMembers() != null ?
                            chatRoom.getMembers().stream()
                                    .map(m -> m.getUser().getId() + ":" + m.getStatus())
                                    .toList() : "null");

            // 권한 검증 (메모리 + DB 이중 확인)
            boolean isMemberInMemory = chatRoom.isMember(senderId);
            boolean isMemberInDb = chatMemberRepository.isActiveMember(roomId, senderId);

            log.debug("🔐 [권한 검증] userId={}, roomId={}", senderId, roomId);
            log.debug("   메모리 확인: {}, DB 확인: {}", isMemberInMemory, isMemberInDb);

            if (!isMemberInDb) {
                log.error("❌ [권한 검증 실패] userId {}는 roomId {} 멤버가 아님 (DB 확인)", senderId, roomId);
                // 디버깅 정보 출력
                if (chatRoom.getMembers() != null) {
                    log.error("   현재 멤버 목록: {}", chatRoom.getMembers().stream()
                            .map(m -> "u" + m.getUser().getId() + "(" + m.getStatus() + ")")
                            .toList());
                }
                throw new RuntimeException("User is not a member of this chat room");
            }
            log.debug("✅ [권한 검증 성공] userId {}는 roomId {} 멤버임 (DB 확인)", senderId, roomId);

            // 2. 사용자 조회
            log.debug("🔍 [단계2] 사용자 조회 중... senderId: {}", senderId);
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> {
                        log.error("❌ [단계2 실패] 사용자를 찾을 수 없음: senderId={}", senderId);
                        return new RuntimeException("User not found: " + senderId);
                    });
            log.debug("✅ [단계2 성공] 사용자 조회됨: senderId={}, nickname={}", sender.getId(), sender.getNickname());

            // 3. 메시지 생성 및 저장
            log.debug("🔍 [단계3] 메시지 엔티티 생성 중...");
            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(sender)
                    .type(type)
                    .content(content)
                    .imageUrl(imageUrl)
                    .cardPayload(cardPayload)
                    .readCount(0)
                    .build();
            log.debug("✅ [단계3] 메시지 엔티티 생성됨");

            log.debug("💾 [단계3] 메시지를 DB에 저장 중...");
            ChatMessage savedMessage = chatMessageRepository.save(message);
            log.info("✅ [단계3 성공] 메시지 DB 저장 완료: messageId={}, createdAt={}",
                    savedMessage.getId(), savedMessage.getCreatedAt());

            // 4. 채팅방 메타데이터 업데이트
            log.debug("🔍 [단계4] 채팅방 메타데이터 업데이트 중...");
            chatRoom.setLastMessageAt(savedMessage.getCreatedAt());
            chatRoom.setLastMessagePreview(truncateContent(content));
            chatRoom.setLastMessageSenderId(senderId);

            log.debug("💾 [단계4] 채팅방 정보 DB에 저장 중...");
            chatRoomRepository.save(chatRoom);
            log.info("✅ [단계4 성공] 채팅방 메타데이터 업데이트 완료: lastMessageAt={}", chatRoom.getLastMessageAt());

            log.info("✅ [메시지 저장 완료] roomId: {}, messageId: {}, sender: {}",
                    roomId, savedMessage.getId(), sender.getNickname());

            return toMessageResponse(savedMessage, senderId);

        } catch (Exception e) {
            log.error("❌ [메시지 저장 중 오류 발생] roomId: {}, senderId: {}, errorMsg: {}",
                    roomId, senderId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 메시지 읽음 처리
     *
     * 마지막으로 읽은 메시지 ID를 저장
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long roomId, Long userId, Long lastReadMessageId) {
        try {
            log.info("📖 [읽음 처리 시작] roomId: {}, userId: {}, lastReadMessageId: {}",
                    roomId, userId, lastReadMessageId);

            log.debug("🔍 [읽음처리-1] 채팅 멤버 조회 중... roomId: {}, userId: {}", roomId, userId);
            ChatMember member = chatMemberRepository.findMember(roomId, userId)
                    .orElseThrow(() -> {
                        log.error("❌ [읽음처리-1 실패] 채팅 멤버를 찾을 수 없음: roomId={}, userId={}", roomId, userId);
                        return new RuntimeException("Chat member not found");
                    });
            log.debug("✅ [읽음처리-1 성공] 멤버 조회됨: currentLastReadId={}", member.getLastReadMessageId());

            // 이전 값이 더 크면 업데이트 안 함 (더 최신을 읽었을 경우)
            if (member.getLastReadMessageId() != null && member.getLastReadMessageId() >= lastReadMessageId) {
                log.debug("⏭️ [읽음처리] 업데이트 스킵: 더 최신 메시지를 이미 읽음 - " +
                        "currentLastReadId: {}, newLastReadId: {}",
                        member.getLastReadMessageId(), lastReadMessageId);
                return;
            }

            log.debug("💾 [읽음처리-2] 마지막 읽은 메시지 ID 업데이트 중... {} -> {}",
                    member.getLastReadMessageId(), lastReadMessageId);
            member.setLastReadMessageId(lastReadMessageId);

            log.debug("💾 [읽음처리-2] DB에 저장 중...");
            chatMemberRepository.save(member);
            log.info("✅ [읽음처리 완료] roomId: {}, userId: {}, lastReadMessageId: {}",
                    roomId, userId, lastReadMessageId);

        } catch (Exception e) {
            log.error("❌ [읽음 처리 중 오류 발생] roomId: {}, userId: {}, errorMsg: {}",
                    roomId, userId, e.getMessage(), e);
            throw e;
        }
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
                .type(message.getType().toString())
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
