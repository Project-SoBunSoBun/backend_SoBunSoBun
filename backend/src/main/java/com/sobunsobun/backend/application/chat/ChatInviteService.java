package com.sobunsobun.backend.application.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatInvite;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.domain.chat.ChatRoomType;
import com.sobunsobun.backend.domain.chat.ChatInviteStatus;
import com.sobunsobun.backend.dto.chat.ChatInviteCancelResponse;
import com.sobunsobun.backend.dto.chat.ChatInviteRequest;
import com.sobunsobun.backend.dto.chat.ChatInviteResponse;
import com.sobunsobun.backend.dto.chat.InviteCardPayload;
import com.sobunsobun.backend.repository.chat.ChatInviteRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatInviteService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatInviteRepository chatInviteRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 그룹 채팅방 초대 발송
     *
     * @param roomId    초대할 채팅방 ID
     * @param inviterId 초대를 보내는 사용자 ID (현재 로그인 유저)
     * @param request   초대 요청 DTO (inviteeId 포함)
     * @return 생성된 초대 정보
     */
    public ChatInviteResponse invite(Long roomId, Long inviterId, ChatInviteRequest request) {
        Long inviteeId = request.getInviteeId();

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 요청자가 ACTIVE 멤버인지 확인
        if (!chatMemberRepository.isActiveMember(roomId, inviterId)) {
            throw new ChatException(ErrorCode.CHAT_MEMBER_NOT_FOUND);
        }

        // 3. 그룹 채팅방인지 확인
        if (chatRoom.getRoomType() != ChatRoomType.GROUP) {
            throw new ChatException(ErrorCode.CHAT_INVALID_ROOM_TYPE);
        }

        // 4. 초대받을 사용자 존재 확인
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(UserException::notFound);

        // 5. invitee가 이미 ACTIVE 멤버인지 확인
        if (chatMemberRepository.isActiveMember(roomId, inviteeId)) {
            throw new ChatException(ErrorCode.CHAT_ALREADY_MEMBER);
        }

        // 6. 동일한 PENDING 초대가 이미 존재하는지 확인
        if (chatInviteRepository.existsPendingInvite(roomId, inviteeId)) {
            throw new ChatException(ErrorCode.CHAT_INVITE_ALREADY_PENDING);
        }

        // 7. 초대 생성 (PENDING, 24시간 유효)
        User inviter = userRepository.getReferenceById(inviterId);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        ChatInvite invite = ChatInvite.builder()
                .chatRoom(chatRoom)
                .inviter(inviter)
                .invitee(invitee)
                .expiresAt(expiresAt)
                .build();

        ChatInvite savedInvite = chatInviteRepository.save(invite);
        log.info("[ChatInvite] 초대 생성 완료 - inviteId: {}, roomId: {}, inviterId: {}, inviteeId: {}",
                savedInvite.getId(), roomId, inviterId, inviteeId);

        // 8. INVITE_CARD 메시지 저장
        saveInviteCardMessage(chatRoom, inviter, savedInvite, expiresAt);

        return ChatInviteResponse.from(savedInvite);
    }

    /**
     * 초대 취소 또는 거절
     *
     * inviter(발신자)와 invitee(수신자) 모두 취소/거절 가능.
     * 최종 status는 REJECTED로 통일.
     *
     * @param inviteId  대상 초대 ID
     * @param requesterId 요청자 ID (현재 로그인 유저)
     * @return 변경된 초대 정보
     */
    public ChatInviteCancelResponse cancelOrRejectInvite(Long inviteId, Long requesterId) {
        // 1. 초대 조회
        ChatInvite invite = chatInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_INVITE_NOT_FOUND));

        // 2. PENDING 상태인지 확인
        if (invite.getStatus() != ChatInviteStatus.PENDING) {
            throw new ChatException(ErrorCode.CHAT_INVITE_NOT_PENDING);
        }

        // 3. 요청자가 inviter 또는 invitee인지 확인
        boolean isInviter = invite.getInviter().getId().equals(requesterId);
        boolean isInvitee = invite.getInvitee().getId().equals(requesterId);
        if (!isInviter && !isInvitee) {
            throw new ChatException(ErrorCode.CHAT_INVITE_ACCESS_DENIED);
        }

        // 4. status를 REJECTED로 변경 (JPA Auditing이 updatedAt 자동 갱신)
        invite.reject();
        log.info("[ChatInvite] 초대 취소/거절 완료 - inviteId: {}, requesterId: {}", inviteId, requesterId);

        return ChatInviteCancelResponse.from(invite);
    }

    private void saveInviteCardMessage(ChatRoom chatRoom, User inviter, ChatInvite invite, LocalDateTime expiresAt) {
        try {
            Long groupPostId = chatRoom.getGroupPost() != null ? chatRoom.getGroupPost().getId() : null;
            String groupPostTitle = chatRoom.getGroupPost() != null ? chatRoom.getGroupPost().getTitle() : null;

            InviteCardPayload payload = InviteCardPayload.builder()
                    .inviteId(invite.getId())
                    .inviterId(inviter.getId())
                    .inviterName(inviter.getNickname())
                    .inviterProfileUrl(inviter.getProfileImageUrl())
                    .groupPostId(groupPostId)
                    .groupPostTitle(groupPostTitle)
                    .expiresAt(expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();

            String cardPayloadJson = objectMapper.writeValueAsString(payload);

            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(inviter)
                    .type(ChatMessageType.INVITE_CARD)
                    .cardPayload(cardPayloadJson)
                    .build();

            chatMessageRepository.save(message);
            log.info("[ChatInvite] INVITE_CARD 메시지 저장 완료 - roomId: {}", chatRoom.getId());

        } catch (JsonProcessingException e) {
            log.warn("[ChatInvite] INVITE_CARD payload 직렬화 실패 - inviteId: {}", invite.getId(), e);
        }
    }
}
