package com.sobunsobun.backend.application.chat;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.SettlementStatus;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatMemberStatus;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.chat.KickMemberResponse;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.SettlementRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMemberService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GroupPostRepository groupPostRepository;
    private final SettlementRepository settlementRepository;

    /**
     * 방장이 특정 멤버를 강퇴시킵니다.
     *
     * @param roomId       채팅방 ID
     * @param requesterId  요청자 ID (방장이어야 함)
     * @param targetUserId 강퇴 대상 사용자 ID
     * @return 강퇴 결과
     */
    public KickMemberResponse kickMember(Long roomId, Long requesterId, Long targetUserId) {

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 방장인지 확인
        if (!chatRoom.getOwner().getId().equals(requesterId)) {
            throw new ChatException(ErrorCode.CHAT_NOT_OWNER);
        }

        // 3. 방장 본인 강퇴 시도 차단
        if (targetUserId.equals(requesterId)) {
            throw new ChatException(ErrorCode.CHAT_CANNOT_KICK_OWNER);
        }

        // 4. 대상 멤버 조회
        ChatMember targetMember = chatMemberRepository.findMember(roomId, targetUserId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_TARGET_NOT_MEMBER));

        // 5. 대상이 ACTIVE 상태인지 확인
        if (targetMember.getStatus() != ChatMemberStatus.ACTIVE) {
            throw new ChatException(ErrorCode.CHAT_MEMBER_ALREADY_LEFT);
        }

        // 정산 진행 중(PENDING) 강퇴 차단
        if (chatRoom.getGroupPost() != null) {
            boolean settlementPending = settlementRepository.existsByGroupPostIdAndStatus(
                    chatRoom.getGroupPost().getId(), SettlementStatus.PENDING);
            if (settlementPending) {
                log.warn("[강퇴 차단] 정산 진행 중 - roomId: {}, targetUserId: {}", roomId, targetUserId);
                throw new ChatException(ErrorCode.CHAT_SETTLEMENT_IN_PROGRESS);
            }
        }

        // 6. chat_member.status를 REVOKED로 변경 (JPA dirty checking으로 자동 저장)
        targetMember.setStatus(ChatMemberStatus.REVOKED);
        String targetNickname = targetMember.getUser().getNickname();
        log.info("[ChatMember] 강퇴 처리 - roomId: {}, targetUserId: {}, nickname: {}",
                roomId, targetUserId, targetNickname);

        // 7. group_post.joined_members 감소
        Integer remainingMembers = decrementJoinedMembers(chatRoom);

        // 8. SYSTEM 메시지 삽입
        String systemContent = targetNickname + "님이 방장에 의해 퇴장되었습니다.";
        ChatMessage systemMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(null)
                .type(ChatMessageType.SYSTEM)
                .content(systemContent)
                .build();
        chatMessageRepository.save(systemMessage);
        log.info("[ChatMember] 시스템 메시지 저장 - roomId: {}, content: {}", roomId, systemContent);

        return KickMemberResponse.builder()
                .roomId(roomId)
                .kickedUserId(targetUserId)
                .kickedUserNickname(targetNickname)
                .remainingMembers(remainingMembers)
                .build();
    }

    /**
     * group_post의 joined_members를 MAX(0, joined_members - 1)로 감소시킵니다.
     * group_post_id가 없는 채팅방이면 null을 반환합니다.
     */
    private Integer decrementJoinedMembers(ChatRoom chatRoom) {
        GroupPost groupPost = chatRoom.getGroupPost();
        if (groupPost == null) {
            return null;
        }

        GroupPost loadedPost = groupPostRepository.findById(groupPost.getId()).orElse(null);
        if (loadedPost == null) {
            return null;
        }

        int updated = Math.max(0, loadedPost.getJoinedMembers() - 1);
        loadedPost.setJoinedMembers(updated);
        log.info("[ChatMember] joined_members 감소 - groupPostId: {}, remaining: {}", loadedPost.getId(), updated);
        return updated;
    }
}
