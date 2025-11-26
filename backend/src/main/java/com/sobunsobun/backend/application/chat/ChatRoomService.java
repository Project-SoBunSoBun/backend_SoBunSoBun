package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreateChatRoomRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomType;
import com.sobunsobun.backend.exception.ChatAuthException;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    private final UserRepository userRepository;


    @Transactional
    public ChatRoomResponse createChatRoom(Long ownerId, CreateChatRoomRequest request) {
        Set<Long> memberIds = new HashSet<>(request.getMemberIds());
        memberIds.add(ownerId);

        if (request.getType() == ChatRoomType.PRIVATE && memberIds.size() != 2) {
            throw new IllegalArgumentException("1:1 채팅방은 정확히 두 명이어야 합니다.");
        }

        ChatRoom room = new ChatRoom(request.getTitle(), request.getType(), ownerId, request.getPostId());
        room = chatRoomRepository.save(room);

        List<ChatMember> members = saveMembers(room.getId(), memberIds);

        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .postId(request.getPostId())
                .title(room.getTitle())
                .chatMembers(members)
                .build();
    }

    @Transactional
    public List<ChatMember> saveMembers(Long roomId, Set<Long> memberIds) {
        List<User> users = userRepository.findAllById(memberIds);

        if (users.size() != memberIds.size()) {
            throw new EntityNotFoundException("존재하지 않는 유저가 포함되어 있습니다.");
        }

        List<ChatMember> members = users.stream()
                .map(user -> ChatMember.builder()
                        .roomId(roomId)
                        .member(user)
                        .build())
                .toList();

        return chatMemberRepository.saveAll(members);
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getMyRoom(Long userId, Long roomId) {
        List<ChatMember> members = chatMemberRepository.findByRoomId(roomId);

        boolean isMember = members.stream()
                .anyMatch(cm -> cm.getMember().getId().equals(userId));

        if (!isMember) {
            throw new ChatAuthException("해당 채팅방에 접근할 권한이 없습니다.");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new EntityNotFoundException("존재하지 않는 채팅방입니다.")
        );

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getId())
                .postId(chatRoom.getPostId())
                .title(chatRoom.getTitle())
                .chatMembers(members)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        List<ChatMember> Members = chatMemberRepository.findByMemberId(userId);

        if (Members.isEmpty()) {
            return List.of();
        }

        Set<Long> roomIds = Members.stream()
                .map(ChatMember::getRoomId)
                .collect(Collectors.toSet());

        List<ChatRoom> rooms = chatRoomRepository.findAllById(roomIds);
        List<ChatMember> allMembers = chatMemberRepository.findByRoomIdIn(roomIds);

        Map<Long, List<ChatMember>> membersByRoom = allMembers.stream()
                .collect(Collectors.groupingBy(ChatMember::getRoomId));

        return rooms.stream()
                .map(room -> ChatRoomResponse.builder()
                        .roomId(room.getId())
                        .postId(room.getPostId())
                        .title(room.getTitle())
                        .chatMembers(
                                membersByRoom.getOrDefault(room.getId(), List.of())
                        )
                        .build()
                )
                .toList();
    }

    //void invite(Long roomId, Long targetUserId);
}
