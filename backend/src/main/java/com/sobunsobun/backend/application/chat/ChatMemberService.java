package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.chat.ChatMemberResponse;
import com.sobunsobun.backend.dto.chat.InviteMemberRequest;
import com.sobunsobun.backend.entity.chat.ChatMember;
import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatMemberRole;
import com.sobunsobun.backend.enumClass.ChatMemberStatus;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final GroupPostRepository groupPostRepository;

    /**
     * 채팅방 기본 최대 인원
     */
    private static final int DEFAULT_MAX_MEMBERS = 30;

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

    @Transactional
    public ChatMemberResponse inviteMember(Long roomId, Long ownerId, InviteMemberRequest request) {
        validateRoomOwner(roomId, ownerId);
        validateMemberLimit(roomId, ownerId);

        Long memberId = request.getMemberId();

        // 초대할 멤버가 존재하는지 확인
        User invitedUser = userRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저입니다."));

        // 이미 멤버인지 확인 (ACTIVE 또는 INVITED 상태)
        boolean isAlreadyMember = chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, memberId, ChatMemberStatus.ACTIVE);
        if (isAlreadyMember) {
            throw new IllegalArgumentException("이미 채팅방의 활성 멤버입니다.");
        }

        boolean isAlreadyInvited = chatMemberRepository.existsByRoomIdAndMemberIdAndStatus(
                roomId, memberId, ChatMemberStatus.INVITED);
        if (isAlreadyInvited) {
            throw new IllegalArgumentException("이미 초대된 멤버입니다.");
        }

        // 멤버를 INVITED 상태로 저장 (초대된 멤버는 항상 MEMBER 역할)
        ChatMember invitedMember = ChatMember.builder()
                .roomId(roomId)
                .member(invitedUser)
                .role(ChatMemberRole.MEMBER)
                .status(ChatMemberStatus.INVITED)
                .build();

        ChatMember saved = chatMemberRepository.save(invitedMember);
        return ChatMemberResponse.from(saved);
    }


    @Transactional(readOnly = true)
    public List<ChatMemberResponse> getInvitedMembers(Long roomId) {
        return chatMemberRepository.findByRoomIdAndStatus(roomId, ChatMemberStatus.INVITED)
                .stream()
                .map(ChatMemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMemberResponse> getInvitationsByUserId(Long userId) {
        return chatMemberRepository.findByMemberIdAndStatus(userId, ChatMemberStatus.INVITED)
                .stream()
                .map(ChatMemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteInvitation(Long roomId, Long userId, Long targetMemberId) {
        ChatMember invitation = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                roomId, targetMemberId, ChatMemberStatus.INVITED)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));

        boolean isOwner = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.ACTIVE)
                .map(member -> member.getRole() == ChatMemberRole.OWNER)
                .orElse(false);

        // 초대자 본인인지 or 방장인지 확인
        if (!userId.equals(targetMemberId) && !isOwner) {
            throw new IllegalArgumentException("초대를 삭제할 권한이 없습니다.");
        }

        // 하드 삭제
        chatMemberRepository.delete(invitation);
    }

    @Transactional
    public ChatMemberResponse acceptInvitation(Long roomId, Long inviteeId) {
        ChatMember invitation = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, inviteeId, ChatMemberStatus.INVITED)
                .orElseThrow(() -> new IllegalArgumentException("초대 정보를 찾을 수 없습니다."));

        // INVITED → ACTIVE로 상태 변경
        invitation.setStatus(ChatMemberStatus.ACTIVE);
        ChatMember saved = chatMemberRepository.save(invitation);
        return ChatMemberResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public void validateRoomOwner(Long roomId, Long userId) {
        ChatMember member = chatMemberRepository.findByRoomIdAndMemberIdAndStatus(
                        roomId, userId, ChatMemberStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버가 아닙니다."));

        if (member.getRole() != ChatMemberRole.OWNER) {
            throw new IllegalArgumentException("방장만 사용할 수 있는 기능입니다.");
        }
    }

    @Transactional(readOnly = true)
    public int getTotalMemberCount(Long roomId) {
        List<ChatMemberStatus> statuses = List.of(ChatMemberStatus.INVITED, ChatMemberStatus.ACTIVE);
        return chatMemberRepository.countByRoomIdAndStatusIn(roomId, statuses);
    }

    /**
     * 채팅방 인원 제한 검증
     * 게시글의 maxMembers를 기준으로 초대 가능 여부 확인
     *
     * @param roomId 채팅방 ID
     * @param ownerId 방장 ID
     * @throws EntityNotFoundException 채팅방 또는 게시글을 찾을 수 없는 경우
     * @throws IllegalArgumentException 최대 인원 초과 시
     */
    @Transactional(readOnly = true)
    public void validateMemberLimit(Long roomId, Long ownerId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 채팅방입니다."));

        // 채팅방이 게시글과 연결되어 있지 않은 경우 (일반 채팅방)
        Long postId = chatRoom.getPostId();
        if (postId == null) {
            // 일반 채팅방은 기본 최대 인원으로 제한
            int totalCount = getTotalMemberCount(roomId);
            if (totalCount >= DEFAULT_MAX_MEMBERS) {
                throw new IllegalArgumentException("채팅방 최대 인원(" + DEFAULT_MAX_MEMBERS + "명)을 초과할 수 없습니다.");
            }
            return;
        }

        // 게시글의 최대 인원 조회
        Integer maxMembers = groupPostRepository.findMaxMembersByIdAndOwnerUserId(postId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("연결된 게시글을 찾을 수 없습니다."));

        // maxMembers가 설정되지 않은 경우 기본값 사용
        int limit = (maxMembers != null) ? maxMembers : DEFAULT_MAX_MEMBERS;

        // 현재 멤버 수 확인
        int totalCount = getTotalMemberCount(roomId);
        if (totalCount >= limit) {
            throw new IllegalArgumentException("채팅방 최대 인원(" + limit + "명)을 초과할 수 없습니다.");
        }
    }
}
