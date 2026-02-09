package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.*;
import com.sobunsobun.backend.dto.chat.ChatRoomListItemResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.repository.ChatMemberRepository;
import com.sobunsobun.backend.repository.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
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
 * 채팅방 관련 비즈니스 로직
 *
 * 기능:
 * - 개인 채팅방 생성 (1:1)
 * - 단체 채팅방 생성
 * - 채팅방 목록 조회 (개인/단체 탭)
 * - 채팅방 상세 정보
 * - 채팅방 나가기
 * - 멤버 초대 (단체)
 * - 멤버 강퇴 (단체, 방장만)
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * 1:1 개인 채팅방 생성 또는 조회
     *
     * 이미 존재하면 기존 방을 반환
     * 없으면 새로 생성
     *
     * @param userId1 사용자 ID 1 (initiator)
     * @param userId2 사용자 ID 2 (recipient)
     * @return 채팅방 (생성 또는 기존)
     * @throws ChatException 사용자 없음
     */
    public ChatRoom createOrGetPrivateChatRoom(Long userId1, Long userId2) {
        // 사용자 존재 확인
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));

        // 기존 1:1 채팅방 조회
        var existingRoom = chatRoomRepository.findPrivateChatRoom(userId1, userId2);
        if (existingRoom.isPresent()) {
            log.debug("기존 개인 채팅방 사용 - roomId: {}", existingRoom.get().getId());
            return existingRoom.get();
        }

        // 새 채팅방 생성
        String roomName = user1.getNickname() + " & " + user2.getNickname();
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .roomType(ChatRoomType.PRIVATE)
                .owner(user1)  // userId1을 방장으로 지정
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // 두 사용자를 멤버로 추가
        ChatMember member1 = ChatMember.builder()
                .chatRoom(savedRoom)
                .user(user1)
                .status(ChatMemberStatus.ACTIVE)
                .build();
        ChatMember member2 = ChatMember.builder()
                .chatRoom(savedRoom)
                .user(user2)
                .status(ChatMemberStatus.ACTIVE)
                .build();

        chatMemberRepository.save(member1);
        chatMemberRepository.save(member2);

        log.info("개인 채팅방 생성 - roomId: {}, user1: {}, user2: {}",
                savedRoom.getId(), userId1, userId2);

        return savedRoom;
    }

    /**
     * 단체 채팅방 생성
     *
     * @param groupPostId 모임 ID
     * @param roomName 채팅방 이름
     * @param ownerId 방장 (모임 주최자)
     * @return 생성된 채팅방
     * @throws ChatException 사용자 없음
     */
    public ChatRoom createGroupChatRoom(Long groupPostId, String roomName, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .roomType(ChatRoomType.GROUP)
                .owner(owner)
                .groupPostId(groupPostId)
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        // 방장을 멤버로 추가
        ChatMember ownerMember = ChatMember.builder()
                .chatRoom(savedRoom)
                .user(owner)
                .status(ChatMemberStatus.ACTIVE)
                .build();
        chatMemberRepository.save(ownerMember);

        log.info("단체 채팅방 생성 - roomId: {}, groupPostId: {}, ownerId: {}",
                savedRoom.getId(), groupPostId, ownerId);

        return savedRoom;
    }

    /**
     * 사용자의 채팅방 목록 조회 (전체 타입)
     *
     * 최신순 정렬 (lastMessageAt DESC)
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 채팅방 목록 (응답 DTO)
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomListItemResponse> getUserChatRooms(Long userId, Pageable pageable) {
        Page<ChatRoom> chatRooms = chatRoomRepository.findUserChatRooms(userId, pageable);
        return chatRooms.map(room -> toChatRoomListItem(room, userId));
    }

    /**
     * 사용자의 채팅방 목록 조회 (타입별)
     *
     * @param userId 사용자 ID
     * @param roomType 채팅방 타입 (PRIVATE, GROUP)
     * @param pageable 페이징 정보
     * @return 채팅방 목록 (응답 DTO)
     */
    @Transactional(readOnly = true)
    public Page<ChatRoomListItemResponse> getUserChatRoomsByType(
            Long userId,
            ChatRoomType roomType,
            Pageable pageable
    ) {
        Page<ChatRoom> chatRooms = chatRoomRepository.findUserChatRoomsByType(userId, roomType, pageable);
        return chatRooms.map(room -> toChatRoomListItem(room, userId));
    }

    /**
     * 채팅방 상세 정보 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID (권한 검증용)
     * @return 채팅방 상세 정보
     * @throws ChatException 채팅방 없음 또는 권한 없음
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoomDetail(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        // 권한 검증
        if (!chatRoom.isMember(userId)) {
            log.warn("채팅방 조회 권한 없음 - roomId: {}, userId: {}", roomId, userId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        return toChatRoomDetail(chatRoom, userId);
    }

    /**
     * 채팅방 나가기
     *
     * 개인 채팅방: 멤버를 LEFT 상태로 변경 (소프트 삭제)
     * 단체 채팅방: 마찬가지 처리
     * 방장인 경우: 다른 활성 멤버가 있으면 방장 양도, 없으면 방 삭제
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @throws ChatException 채팅방 없음 또는 멤버 아님
     */
    public void leaveChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        ChatMember member = chatMemberRepository.findMember(roomId, userId)
                .orElseThrow(() -> new ChatException(CHAT_MEMBER_NOT_FOUND));

        // 멤버 상태를 LEFT로 변경
        member.setStatus(ChatMemberStatus.LEFT);
        chatMemberRepository.save(member);

        // 방장인 경우 처리
        if (chatRoom.isOwner(userId)) {
            List<ChatMember> activeMembers = chatMemberRepository.findActiveMembers(roomId);
            if (activeMembers.isEmpty()) {
                // 활성 멤버 없으면 채팅방 삭제
                chatRoomRepository.delete(chatRoom);
                log.info("채팅방 삭제 (마지막 멤버 퇴장) - roomId: {}", roomId);
            } else {
                // 다른 활성 멤버가 있으면 첫 번째를 새 방장으로 지정
                chatRoom.setOwner(activeMembers.get(0).getUser());
                chatRoomRepository.save(chatRoom);
                log.info("채팅방 방장 변경 - roomId: {}, newOwnerId: {}", roomId, activeMembers.get(0).getUser().getId());
            }
        }

        log.info("채팅방 퇴장 - roomId: {}, userId: {}", roomId, userId);
    }

    /**
     * 멤버 강퇴 (단체 채팅방에서 방장만)
     *
     * @param roomId 채팅방 ID
     * @param operatorId 작업자 ID (방장)
     * @param targetUserId 강퇴 대상자 ID
     * @throws ChatException 권한 없음 또는 멤버 아님
     */
    public void removeMember(Long roomId, Long operatorId, Long targetUserId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        // 작업자가 방장인지 확인
        if (!chatRoom.isOwner(operatorId)) {
            log.warn("멤버 강퇴 권한 없음 - roomId: {}, operatorId: {}", roomId, operatorId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMember targetMember = chatMemberRepository.findMember(roomId, targetUserId)
                .orElseThrow(() -> new ChatException(CHAT_MEMBER_NOT_FOUND));

        // 멤버 상태를 LEFT로 변경
        targetMember.setStatus(ChatMemberStatus.LEFT);
        chatMemberRepository.save(targetMember);

        log.info("멤버 강퇴 - roomId: {}, targetUserId: {}", roomId, targetUserId);
    }

    /**
     * ChatRoom을 ChatRoomListItemResponse로 변환
     *
     * @param room 채팅방 엔티티
     * @param userId 현재 사용자 ID
     * @return 채팅방 목록 항목
     */
    private ChatRoomListItemResponse toChatRoomListItem(ChatRoom room, Long userId) {
        // unreadCount 계산
        long unreadCount = chatMemberRepository.countUnreadMessages(room.getId(), userId);

        ChatRoomListItemResponse.ChatRoomListItemResponseBuilder builder = ChatRoomListItemResponse.builder()
                .roomId(room.getId())
                .name(room.getName())
                .roomType(room.getRoomType().name())
                .lastMessagePreview(room.getLastMessagePreview())
                .unreadCount(unreadCount)
                .lastMessageAt(room.getLastMessageAt())
                .memberCount(room.getActiveMemberCount());

        // 발송자 이름 추가
        if (room.getLastMessageSenderId() != null) {
            User sender = userRepository.findById(room.getLastMessageSenderId()).orElse(null);
            if (sender != null) {
                builder.lastMessageSenderName(sender.getNickname());
            }
        }

        // 개인 채팅이면 상대방 정보 추가
        if (room.getRoomType() == ChatRoomType.PRIVATE) {
            List<ChatMember> members = room.getMembers();
            for (ChatMember member : members) {
                if (!member.getUser().getId().equals(userId)) {
                    builder.otherUserId(member.getUser().getId())
                           .profileImageUrl(member.getUser().getProfileImageUrl());
                    break;
                }
            }
        }

        return builder.build();
    }

    /**
     * ChatRoom을 ChatRoomResponse로 변환
     *
     * @param room 채팅방 엔티티
     * @param userId 현재 사용자 ID
     * @return 채팅방 상세 정보
     */
    private ChatRoomResponse toChatRoomDetail(ChatRoom room, Long userId) {
        List<ChatRoomResponse.ChatRoomMemberInfo> memberInfos = room.getMembers().stream()
                .filter(m -> m.getStatus() == ChatMemberStatus.ACTIVE)
                .map(m -> ChatRoomResponse.ChatRoomMemberInfo.builder()
                        .userId(m.getUser().getId())
                        .nickname(m.getUser().getNickname())
                        .profileImageUrl(m.getUser().getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());

        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .name(room.getName())
                .roomType(room.getRoomType().name())
                .memberCount(room.getActiveMemberCount())
                .members(memberInfos)
                .isOwner(room.isOwner(userId))
                .createdAt(room.getCreatedAt())
                .build();
    }
}
