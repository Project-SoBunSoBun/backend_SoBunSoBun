package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.*;
import com.sobunsobun.backend.repository.ChatInviteRepository;
import com.sobunsobun.backend.repository.ChatMemberRepository;
import com.sobunsobun.backend.repository.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.sobunsobun.backend.support.exception.ErrorCode.*;

/**
 * 채팅 초대장 관련 비즈니스 로직
 *
 * 기능:
 * - 개인 채팅에서 방장이 초대장 생성
 * - 초대받은 사용자가 수락/거절
 * - 수락 시 해당 단체 채팅방에 멤버 추가
 * - 초대장 목록 조회
 *
 * 초대 흐름:
 * 1. 개인 채팅방에서 방장이 초대 요청
 * 2. ChatInvite 엔티티 생성 + ChatMessage(INVITE_CARD) 생성
 * 3. 초대받은 사용자가 모빌 앱에서 "수락" 클릭
 * 4. ChatMember 추가 + 단체방에 SYSTEM 메시지 ("User joined")
 * 5. 클라이언트가 roomId로 이동
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatInviteService {

    private final ChatInviteRepository chatInviteRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    /**
     * 초대장 생성
     *
     * 개인 채팅에서 방장이 상대방을 단체 채팅으로 초대
     *
     * 흐름:
     * 1. 권한 검증 (개인 채팅의 방장인지)
     * 2. ChatInvite 생성 (PENDING)
     * 3. ChatMessage(INVITE_CARD) 생성 (초대 메시지)
     * 4. 초대장 ID를 cardPayload에 저장
     *
     * @param privateChatRoomId 개인 채팅방 ID
     * @param inviterId 방장 ID
     * @param inviteeId 초대받은 사용자 ID
     * @param targetGroupPostId 초대할 단체 채팅의 모임 ID
     * @return 생성된 초대장
     * @throws ChatException 권한 없음 또는 사용자 없음
     */
    public ChatInvite createInvite(
            Long privateChatRoomId,
            Long inviterId,
            Long inviteeId,
            Long targetGroupPostId
    ) {
        // 개인 채팅방 조회
        ChatRoom privateRoom = chatRoomRepository.findByIdWithMembers(privateChatRoomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        // 권한 검증: 방장인지 확인
        if (!privateRoom.isOwner(inviterId)) {
            log.warn("초대장 생성 권한 없음 - roomId: {}, userId: {}", privateChatRoomId, inviterId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        // 사용자 존재 확인
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));

        // 중복 초대 검증: 같은 채팅방에서 이미 초대한 경우 최신 상태 반환
        var existingInvite = chatInviteRepository.findLatestInviteForRoom(
                privateChatRoomId, inviteeId
        );
        if (existingInvite.isPresent()) {
            ChatInvite latest = existingInvite.get();
            if (latest.getStatus() == ChatInviteStatus.PENDING && !latest.isExpired()) {
                log.warn("이미 초대한 사용자 - roomId: {}, inviteeId: {}",
                        privateChatRoomId, inviteeId);
                return latest;
            }
        }

        // ChatInvite 생성 (7일 만료)
        ChatInvite invite = ChatInvite.builder()
                .chatRoom(privateRoom)
                .inviter(inviter)
                .invitee(invitee)
                .status(ChatInviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        ChatInvite savedInvite = chatInviteRepository.save(invite);

        log.info("초대장 생성 - inviteId: {}, inviterId: {}, inviteeId: {}",
                savedInvite.getId(), inviterId, inviteeId);

        return savedInvite;
    }

    /**
     * 초대 수락
     *
     * 흐름:
     * 1. 초대장 상태 변경 (PENDING → ACCEPTED)
     * 2. 해당 단체 채팅방 조회
     * 3. ChatMember 추가 (ACTIVE)
     * 4. 단체 채팅방에 SYSTEM 메시지 생성 ("User joined")
     *
     * @param inviteId 초대장 ID
     * @param acceptorId 수락 사용자 ID (=초대받은 사용자)
     * @param targetGroupChatRoomId 추가될 단체 채팅방 ID
     * @throws ChatException 초대장 없음, 만료됨, 또는 권한 없음
     */
    public ChatInvite acceptInvite(Long inviteId, Long acceptorId, Long targetGroupChatRoomId) {
        // 초대장 조회
        ChatInvite invite = chatInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ChatException(CHAT_INVITE_NOT_FOUND));

        // 권한 검증: 초대받은 사람이 수락하는지
        if (!invite.getInvitee().getId().equals(acceptorId)) {
            log.warn("초대 수락 권한 없음 - inviteId: {}, acceptorId: {}", inviteId, acceptorId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        // 초대장 만료 확인
        if (invite.isExpired()) {
            log.warn("초대장 만료됨 - inviteId: {}", inviteId);
            throw new ChatException(CHAT_INVITE_EXPIRED);
        }

        // 초대장 상태 변경
        invite.accept();
        chatInviteRepository.save(invite);

        // 단체 채팅방 조회
        ChatRoom targetGroupRoom = chatRoomRepository.findByIdWithMembers(targetGroupChatRoomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        // 이미 멤버인지 확인
        if (targetGroupRoom.isMember(acceptorId)) {
            log.warn("이미 채팅방 멤버 - roomId: {}, userId: {}", targetGroupChatRoomId, acceptorId);
            return invite;
        }

        // ChatMember 추가
        User acceptor = userRepository.findById(acceptorId)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));

        ChatMember newMember = ChatMember.builder()
                .chatRoom(targetGroupRoom)
                .user(acceptor)
                .status(ChatMemberStatus.ACTIVE)
                .build();

        chatMemberRepository.save(newMember);

        // 시스템 메시지 생성
        ChatMessage systemMessage = ChatMessage.createSystemMessage(
                targetGroupRoom,
                acceptor.getNickname() + "님이 참여했습니다."
        );
        // 메시지 저장은 별도 처리 또는 이 서비스에서 직접 처리 가능
        // messageRepository.save(systemMessage);

        log.info("초대 수락 - inviteId: {}, acceptorId: {}, targetRoomId: {}",
                inviteId, acceptorId, targetGroupChatRoomId);

        return invite;
    }

    /**
     * 초대 거절
     *
     * @param inviteId 초대장 ID
     * @param declinerId 거절 사용자 ID
     * @throws ChatException 초대장 없음 또는 권한 없음
     */
    public ChatInvite declineInvite(Long inviteId, Long declinerId) {
        ChatInvite invite = chatInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ChatException(CHAT_INVITE_NOT_FOUND));

        // 권한 검증
        if (!invite.getInvitee().getId().equals(declinerId)) {
            log.warn("초대 거절 권한 없음 - inviteId: {}, declinerId: {}", inviteId, declinerId);
            throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
        }

        // 상태 변경
        invite.decline();
        chatInviteRepository.save(invite);

        log.info("초대 거절 - inviteId: {}, declinerId: {}", inviteId, declinerId);

        return invite;
    }

    /**
     * 사용자가 받은 초대장 목록 (PENDING만)
     *
     * @param userId 사용자 ID
     * @return 대기 중인 초대장 목록
     */
    @Transactional(readOnly = true)
    public List<ChatInvite> getPendingInvites(Long userId) {
        return chatInviteRepository.findPendingInvitesByInvitee(userId);
    }

    /**
     * 사용자가 받은 초대장 개수 (PENDING)
     *
     * @param userId 사용자 ID
     * @return 초대장 개수
     */
    @Transactional(readOnly = true)
    public long getPendingInviteCount(Long userId) {
        return chatInviteRepository.countPendingInvites(userId);
    }

    /**
     * 만료된 초대장 정리 (배치 작업용)
     *
     * 주기적으로 호출하여 만료된 초대장을 마크
     */
    public void expireOldInvites() {
        List<ChatInvite> expiredInvites = chatInviteRepository.findExpiredInvites(LocalDateTime.now());
        expiredInvites.forEach(invite -> {
            invite.setStatus(ChatInviteStatus.EXPIRED);
            chatInviteRepository.save(invite);
        });
        log.info("만료된 초대장 정리 완료 - count: {}", expiredInvites.size());
    }
}
