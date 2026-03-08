package com.sobunsobun.backend.application.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatInvite;
import com.sobunsobun.backend.domain.chat.ChatMember;
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
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

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

        // 8. invitee와의 1:1 채팅방에 INVITE_CARD 메시지 저장 (invitee가 채팅에서 바로 확인)
        Long groupPostId = chatRoom.getGroupPost() != null ? chatRoom.getGroupPost().getId() : null;
        chatMemberRepository.findOneToOneChatRoom(inviterId, inviteeId, groupPostId)
                .ifPresentOrElse(
                        oneToOneRoom -> saveInviteCardMessage(oneToOneRoom, chatRoom, inviter, savedInvite, expiresAt),
                        () -> log.warn("[ChatInvite] 1:1 채팅방 없음, INVITE_CARD 저장 생략 - inviterId: {}, inviteeId: {}", inviterId, inviteeId)
                );

        return ChatInviteResponse.from(savedInvite);
    }

    /**
     * 초대 수락
     *
     * invitee(수신자)만 수락 가능. 수락 시 채팅방 멤버로 추가되고 ENTER 시스템 메시지 발행.
     *
     * @param inviteId    대상 초대 ID
     * @param requesterId 요청자 ID (현재 로그인 유저, invitee여야 함)
     * @return 수락된 초대 정보
     */
    public ChatInviteResponse acceptInvite(Long inviteId, Long requesterId) {
        // 1. 초대 조회
        ChatInvite invite = chatInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_INVITE_NOT_FOUND));

        // 2. PENDING 상태인지 확인
        if (invite.getStatus() != ChatInviteStatus.PENDING) {
            throw new ChatException(ErrorCode.CHAT_INVITE_NOT_PENDING);
        }

        // 3. 만료 여부 확인
        if (invite.isExpired()) {
            throw new ChatException(ErrorCode.CHAT_INVITE_EXPIRED);
        }

        // 4. 요청자가 invitee인지 확인
        if (!invite.getInvitee().getId().equals(requesterId)) {
            throw new ChatException(ErrorCode.CHAT_INVITE_ACCESS_DENIED);
        }

        // 5. 초대 수락 (status → ACCEPTED)
        invite.accept();

        // 6. 채팅방 멤버로 추가 (이미 멤버면 스킵)
        Long roomId = invite.getChatRoom().getId();
        if (!chatMemberRepository.isActiveMember(roomId, requesterId)) {
            ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                    .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
            User invitee = invite.getInvitee();

            ChatMember newMember = chatRoom.addMember(invitee);
            chatMemberRepository.saveAndFlush(newMember);

            // ENTER 시스템 메시지 발행
            chatMessageService.publishSystemMessage(
                    roomId,
                    invitee,
                    ChatMessageType.ENTER,
                    invitee.getNickname() + "님이 입장했습니다."
            );
        }

        log.info("[ChatInvite] 초대 수락 완료 - inviteId: {}, roomId: {}, inviteeId: {}",
                inviteId, roomId, requesterId);

        return ChatInviteResponse.from(invite);
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

    /**
     * 1:1 채팅방에 INVITE_CARD 메시지 저장 및 브로드캐스트
     *
     * @param oneToOneRoom inviter↔invitee 간 1:1 채팅방 (카드를 보여줄 방)
     * @param groupRoom    초대 대상 그룹 채팅방 (groupPostId/Title 추출용)
     */
    private void saveInviteCardMessage(ChatRoom oneToOneRoom, ChatRoom groupRoom, User inviter, ChatInvite invite, LocalDateTime expiresAt) {
        try {
            Long groupPostId = groupRoom.getGroupPost() != null ? groupRoom.getGroupPost().getId() : null;
            String groupPostTitle = groupRoom.getGroupPost() != null ? groupRoom.getGroupPost().getTitle() : null;

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

            // 1:1 채팅방에 저장 → invitee가 채팅에서 바로 확인 가능
            // saveMessage()가 DB 저장 + Redis 브로드캐스트 + 채팅 목록 업데이트 일괄 처리
            chatMessageService.saveMessage(
                    oneToOneRoom.getId(),
                    inviter.getId(),
                    ChatMessageType.INVITE_CARD,
                    null,
                    null,
                    cardPayloadJson
            );
            log.info("[ChatInvite] INVITE_CARD 저장 완료 - 1:1roomId: {}, groupRoomId: {}", oneToOneRoom.getId(), groupRoom.getId());

        } catch (JsonProcessingException e) {
            log.warn("[ChatInvite] INVITE_CARD payload 직렬화 실패 - inviteId: {}", invite.getId(), e);
        }
    }
}
