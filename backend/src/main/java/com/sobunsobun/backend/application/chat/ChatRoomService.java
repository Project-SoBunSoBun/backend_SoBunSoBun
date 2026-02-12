package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.domain.chat.ChatRoomType;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 채팅방 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * 개인 채팅방 생성 또는 조회
     */
    public ChatRoom getOrCreatePrivateChatRoom(Long userId1, Long userId2) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("🔒 [개인 채팅방 생성/조회 시작] userId1: {}, userId2: {}", userId1, userId2);

            // 기존 채팅방 조회
            log.debug("🔍 [단계1] 기존 개인 채팅방 조회 중...");
            Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoom(userId1, userId2);
            if (existingRoom.isPresent()) {
                log.info("✅ [단계1 완료] 기존 개인 채팅방 발견 - roomId: {}", existingRoom.get().getId());
                log.info("═════════════════════════════════════════════════════════════");
                return existingRoom.get();
            }
            log.info("ℹ️ [단계1 완료] 기존 채팅방 없음 - 새로 생성 필요");

            // 새 채팅방 생성
            log.debug("🔍 [단계2] User1 조회 중... userId: {}", userId1);
            User user1 = userRepository.findById(userId1)
                    .orElseThrow(() -> {
                        log.error("❌ [단계2 실패] User1을 찾을 수 없음: userId={}", userId1);
                        return new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId1 + ")");
                    });
            log.info("✅ [단계2 완료] User1 조회됨: {}", user1.getNickname());

            log.debug("🔍 [단계3] User2 조회 중... userId: {}", userId2);
            User user2 = userRepository.findById(userId2)
                    .orElseThrow(() -> {
                        log.error("❌ [단계3 실패] User2를 찾을 수 없음: userId={}", userId2);
                        return new IllegalArgumentException("존재하지 않는 사용자입니다 (userId: " + userId2 + ")");
                    });
            log.info("✅ [단계3 완료] User2 조회됨: {}", user2.getNickname());

            log.debug("🔨 [단계4] ChatRoom 엔티티 생성 중...");
            ChatRoom chatRoom = ChatRoom.builder()
                    .name(user2.getNickname())  // 개인 채팅방은 상대방 이름으로 표시
                    .roomType(ChatRoomType.PRIVATE)
                    .owner(user1)
                    .messageCount(0L)
                    .build();
            log.info("✅ [단계4 완료] ChatRoom 엔티티 생성됨");

            log.debug("💾 [단계5] ChatRoom DB 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info("✅ [단계5 완료] ChatRoom DB 저장됨 - roomId: {}", savedRoom.getId());

            // 두 사용자를 멤버로 추가
            log.debug("🔨 [단계6] ChatMember 엔티티 생성 중...");
            ChatMember member1 = savedRoom.addMember(user1);
            ChatMember member2 = savedRoom.addMember(user2);
            log.info("✅ [단계6 완료] ChatMember 엔티티 생성됨");

            log.debug("💾 [단계7] ChatMember DB 저장 중... member count: 2");
            // 멤버 저장
            chatMemberRepository.saveAndFlush(member1);
            chatMemberRepository.saveAndFlush(member2);
            log.info("✅ [단계7 완료] ChatMember DB 저장됨");

            log.info("✅ [개인 채팅방 생성 완료] roomId: {}, owner: {}, member: {} <-> {}",
                    savedRoom.getId(), user1.getNickname(), user1.getNickname(), user2.getNickname());
            log.info("═════════════════════════════════════════════════════════════");

            return savedRoom;

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [개인 채팅방 생성 실패] 유효하지 않은 요청 - userId1: {}, userId2: {}", userId1, userId2);
            log.warn("   - errorMsg: {}", e.getMessage());
            throw e;  // 그대로 전파하여 컨트롤러에서 처리
        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [개인 채팅방 생성 실패] 예외 발생", e);
            log.error("   - userId1: {}, userId2: {}", userId1, userId2);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");
            throw new RuntimeException("개인 채팅방 생성 실패: " + e.getMessage(), e);
        }
    }


    /**
     * 채팅방에 멤버 추가
     */
    public void addMember(Long roomId, Long userId) {
        log.info("➕ 멤버 추가 - roomId: {}, userId: {}", roomId, userId);

        try {
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + roomId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 이미 멤버인지 확인
            if (chatRoom.isMember(userId)) {
                log.warn("⚠️ 이미 멤버임 - roomId: {}, userId: {}", roomId, userId);
                return;
            }

            ChatMember newMember = chatRoom.addMember(user);
            // 명시적 저장
            log.info("💾 ChatMember 저장 중...");
            chatMemberRepository.saveAndFlush(newMember);

            log.info("✅ 멤버 추가 완료 - roomId: {}, userId: {}, memberId: {}",
                    roomId, userId, newMember.getId());

        } catch (Exception e) {
            log.error("❌ 멤버 추가 실패 - roomId: {}, userId: {}", roomId, userId, e);
            throw new RuntimeException("멤버 추가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 채팅방을 응답 DTO로 변환 (unreadCount 포함)
     */
    public ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, Long userId) {
        // 안 읽은 메시지 개수 조회
        long unreadCount = chatMemberRepository.countUnreadMessages(chatRoom.getId(), userId);

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .roomType(chatRoom.getRoomType().toString())
                .ownerId(chatRoom.getOwner() != null ? chatRoom.getOwner().getId() : null)
                .memberCount(chatRoom.getMembers().size())
                .unreadCount(unreadCount)
                .lastMessagePreview(chatRoom.getLastMessagePreview())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .build();
    }
}
