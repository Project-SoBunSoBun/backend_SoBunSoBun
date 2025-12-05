package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void validateMembership(Long roomId, Long userId) {
        boolean isMember = chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, userId, ChatMemberStatus.ACTIVE);
        if (!isMember) {
            throw new IllegalArgumentException("채팅방 멤버만 접근할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long roomId, Long userId) {
        return chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, userId, ChatMemberStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getMembersByRoomId(Long roomId) {
        return chatMemberRepository.findByRoomIdAndStatus(roomId, ChatMemberStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getMembersByUserId(Long userId) {
        return chatMemberRepository.findByMemberIdAndStatus(userId, ChatMemberStatus.ACTIVE);
    }

    @Transactional
    public List<ChatMember> saveMembers(Long roomId, Long ownerId, Set<Long> memberIds) {
        List<User> users = userRepository.findAllById(memberIds);

        if (users.size() != memberIds.size()) {
            throw new EntityNotFoundException("존재하지 않는 유저가 포함되어 있습니다.");
        }

        // OWNER 먼저, 그 다음 MEMBER 순서로 정렬
        List<User> sortedUsers = users.stream()
                .sorted((u1, u2) -> {
                    if (u1.getId().equals(ownerId)) return -1;
                    if (u2.getId().equals(ownerId)) return 1;
                    return u1.getId().compareTo(u2.getId());
                })
                .toList();

        // 매핑: owner → OWNER, others → MEMBER
        List<ChatMember> members = sortedUsers.stream()
                .map(user -> ChatMember.builder()
                        .roomId(roomId)
                        .member(user)
                        .role(user.getId().equals(ownerId)
                                ? ChatMemberRole.OWNER
                                : ChatMemberRole.MEMBER)
                        .status(ChatMemberStatus.ACTIVE)
                        .build()
                )
                .toList();

        return chatMemberRepository.saveAll(members);
    }

    @Transactional
    public void removeMember(Long roomId, Long userId) {
        ChatMember member = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        // Soft delete: status를 LEFT로 변경하고 leftAt 시간 설정
        member.setStatus(ChatMemberStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());
        chatMemberRepository.save(member);
    }

    @Transactional
    public void removeMemberFromRooms(List<Long> roomIds, Long userId) {
        List<ChatMember> members = chatMemberRepository.findByRoomIdInAndStatus(
                roomIds, ChatMemberStatus.ACTIVE);

        LocalDateTime now = LocalDateTime.now();
        members.stream()
                .filter(member -> member.getMember().getId().equals(userId))
                .forEach(member -> {
                    member.setStatus(ChatMemberStatus.LEFT);
                    member.setLeftAt(now);
                });

        chatMemberRepository.saveAll(members);
    }

    @Transactional(readOnly = true)
    public long countMembersInRoom(Long roomId) {
        return chatMemberRepository.countByRoomIdAndStatus(roomId, ChatMemberStatus.ACTIVE);
    }
}
