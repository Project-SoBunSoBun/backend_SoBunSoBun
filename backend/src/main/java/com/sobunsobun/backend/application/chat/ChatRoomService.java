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
        log.info("🔒 개인 채팅방 조회/생성 - userId1: {}, userId2: {}", userId1, userId2);

        try {
            // 기존 채팅방 조회
            Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoom(userId1, userId2);
            if (existingRoom.isPresent()) {
                log.info("✅ 기존 개인 채팅방 발견 - roomId: {}", existingRoom.get().getId());
                return existingRoom.get();
            }

            // 새 채팅방 생성
            User user1 = userRepository.findById(userId1)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId1));
            User user2 = userRepository.findById(userId2)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId2));

            ChatRoom chatRoom = ChatRoom.builder()
                    .name(user2.getNickname())  // 개인 채팅방은 상대방 이름으로 표시
                    .roomType(ChatRoomType.PRIVATE)
                    .owner(user1)
                    .messageCount(0L)
                    .build();

            log.info("💾 ChatRoom 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info("✅ ChatRoom 저장 완료 - roomId: {}", savedRoom.getId());

            // 두 사용자를 멤버로 추가
            ChatMember member1 = savedRoom.addMember(user1);
            ChatMember member2 = savedRoom.addMember(user2);

            log.info("💾 ChatMember 저장 중...");
            // 멤버 저장
            chatMemberRepository.saveAndFlush(member1);
            chatMemberRepository.saveAndFlush(member2);

            log.info("✅ 개인 채팅방 생성 완료 - roomId: {}, members: 2", savedRoom.getId());
            return savedRoom;

        } catch (Exception e) {
            log.error("❌ 개인 채팅방 생성 실패 - userId1: {}, userId2: {}", userId1, userId2, e);
            throw new RuntimeException("개인 채팅방 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 단체 채팅방 생성
     */
    public ChatRoom createGroupChatRoom(String roomName, Long ownerId, Long groupPostId) {
        log.info("👥 단체 채팅방 생성 - roomName: {}, ownerId: {}, groupPostId: {}",
                roomName, ownerId, groupPostId);

        try {
            User owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + ownerId));

            ChatRoom chatRoom = ChatRoom.builder()
                    .name(roomName)
                    .roomType(ChatRoomType.GROUP)
                    .owner(owner)
                    .groupPostId(groupPostId)
                    .messageCount(0L)
                    .build();

            log.info("💾 ChatRoom 저장 중...");
            ChatRoom savedRoom = chatRoomRepository.saveAndFlush(chatRoom);
            log.info("✅ ChatRoom 저장 완료 - roomId: {}", savedRoom.getId());

            // 방장을 멤버로 추가
            ChatMember ownerMember = savedRoom.addMember(owner);
            log.info("💾 ChatMember 저장 중...");
            chatMemberRepository.saveAndFlush(ownerMember);

            log.info("✅ 단체 채팅방 생성 완료 - roomId: {}, owner: {}", savedRoom.getId(), owner.getNickname());
            return savedRoom;

        } catch (Exception e) {
            log.error("❌ 단체 채팅방 생성 실패 - roomName: {}, ownerId: {}, groupPostId: {}",
                    roomName, ownerId, groupPostId, e);
            throw new RuntimeException("단체 채팅방 생성 실패: " + e.getMessage(), e);
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
