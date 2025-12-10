package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatMemberRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        member.setLeftAt(Instant.now());
        chatMemberRepository.save(member);
    }

    @Transactional
    public void removeMemberFromRooms(List<Long> roomIds, Long userId) {
        List<ChatMember> members = chatMemberRepository.findByRoomIdInAndStatus(
                roomIds, ChatMemberStatus.ACTIVE);

        Instant now = Instant.now();
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

    /**
     * 멤버 초대 (초대 발송은 message가 처리)
     * @param roomId 채팅방 ID
     * @param ownerId 요청자(방장) ID
     * @param request 초대할 멤버 정보
     */
    @Transactional
    public ChatMember inviteMember(Long roomId, Long ownerId, ChatMemberRequest request) {
        // 방장 권한 확인
        validateRoomOwner(roomId, ownerId);

        // 초대할 멤버가 존재하는지 확인
        User invitedUser = userRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저입니다."));

        // 이미 멤버인지 확인 (ACTIVE 또는 INVITED 상태)
        boolean isAlreadyMember = chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, request.getMemberId(), ChatMemberStatus.ACTIVE);
        if (isAlreadyMember) {
            throw new IllegalArgumentException("이미 채팅방의 활성 멤버입니다.");
        }

        boolean isAlreadyInvited = chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, request.getMemberId(), ChatMemberStatus.INVITED);
        if (isAlreadyInvited) {
            throw new IllegalArgumentException("이미 초대된 멤버입니다.");
        }

        // 멤버를 INVITED 상태로 저장
        ChatMember invitedMember = ChatMember.builder()
                .roomId(roomId)
                .member(invitedUser)
                .role(request.getChatMemberRole() != null ? request.getChatMemberRole() : ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();

        return chatMemberRepository.save(invitedMember);
    }

    /**
     * 초대된 멤버 목록 조회 (특정 채팅방)
     * @param roomId 채팅방 ID
     * @return 초대된 멤버 목록
     */
    @Transactional(readOnly = true)
    public List<ChatMember> getInvitedMembers(Long roomId) {
        return chatMemberRepository.findByRoomIdAndStatus(roomId, ChatMemberStatus.INVITED);
    }

    /**
     * 사용자가 받은 초대 목록 조회
     * @param userId 사용자 ID
     * @return 사용자가 받은 초대 목록
     */
    @Transactional(readOnly = true)
    public List<ChatMember> getInvitationsByUserId(Long userId) {
        return chatMemberRepository.findByMemberIdAndStatus(userId, ChatMemberStatus.INVITED);
    }

    /**
     * 초대 거절 또는 취소
     * @param roomId 채팅방 ID
     * @param userId 요청자 ID (초대받은 사람 또는 방장)
     * @param targetMemberId 대상 멤버 ID
     */
    @Transactional
    public void deleteInvitation(Long roomId, Long userId, Long targetMemberId) {
        ChatMember invitation = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, targetMemberId, ChatMemberStatus.INVITED)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));

        // 요청자가 초대받은 본인이거나 방장인 경우만 삭제 가능
        boolean isOwner = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.ACTIVE)
                .map(member -> member.getRole() == ChatMemberRole.OWNER)
                .orElse(false);

        if (!userId.equals(targetMemberId) && !isOwner) {
            throw new IllegalArgumentException("초대를 삭제할 권한이 없습니다.");
        }

        // 하드 삭제
        chatMemberRepository.delete(invitation);
    }

    /**
     * 초대 수락 (채팅방 가입)
     * @param roomId 채팅방 ID
     * @param userId 초대받은 사용자 ID
     */
    @Transactional
    public ChatMember acceptInvitation(Long roomId, Long userId) {
        ChatMember invitation = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.INVITED)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));

        // INVITED → ACTIVE로 상태 변경
        invitation.setStatus(ChatMemberStatus.ACTIVE);
        return chatMemberRepository.save(invitation);
    }

    /**
     * 방장 권한 확인
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    @Transactional(readOnly = true)
    public void validateRoomOwner(Long roomId, Long userId) {
        ChatMember member = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        if (member.getRole() != ChatMemberRole.OWNER) {
            throw new IllegalArgumentException("방장만 사용할 수 있는 기능입니다.");
        }
    }
}
