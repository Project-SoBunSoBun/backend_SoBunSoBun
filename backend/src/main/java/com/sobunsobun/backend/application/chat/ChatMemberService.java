package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void validateMembership(Long roomId, Long userId) {
        boolean isMember = chatMemberRepository.existsByRoomIdAndMemberId(roomId, userId);
        if (!isMember) {
            throw new IllegalArgumentException("채팅방 멤버만 접근할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long roomId, Long userId) {
        return chatMemberRepository.existsByRoomIdAndMemberId(roomId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getMembersByRoomId(Long roomId) {
        return chatMemberRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatMember> getMembersByUserId(Long userId) {
        return chatMemberRepository.findByMemberId(userId);
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
                        .build()
                )
                .toList();

        return chatMemberRepository.saveAll(members);
    }
}
