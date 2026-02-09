package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatInviteService;
import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.domain.ChatInvite;
import com.sobunsobun.backend.domain.ChatMessage;
import com.sobunsobun.backend.dto.chat.AcceptChatInviteRequest;
import com.sobunsobun.backend.dto.chat.ChatInviteResponse;
import com.sobunsobun.backend.dto.chat.CreateChatInviteRequest;
import com.sobunsobun.backend.repository.ChatMessageRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 초대 REST API Controller
 *
 * 엔드포인트:
 * - POST /api/v1/chat/invites - 초대장 생성 (방장만)
 * - GET /api/v1/chat/invites - 받은 초대장 목록
 * - PUT /api/v1/chat/invites/{id}/accept - 초대 수락
 * - PUT /api/v1/chat/invites/{id}/decline - 초대 거절
 *
 * 초대 흐름:
 * 1. 개인 채팅 방장이 초대장 생성 + INVITE_CARD 메시지 전송
 * 2. 상대방이 받은 초대장 목록에서 확인
 * 3. "수락" 시 단체 채팅방 멤버 추가
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat/invites")
@Tag(name = "채팅 초대 관리", description = "개인 채팅에서 단체 모임으로 초대하는 기능")
@RequiredArgsConstructor
public class ChatInviteRestController {

    private final ChatInviteService chatInviteService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 초대장 생성
     *
     * 개인 채팅의 방장이 상대방을 단체 채팅으로 초대
     *
     * 흐름:
     * 1. ChatInvite 생성
     * 2. ChatMessage(INVITE_CARD) 생성
     * 3. cardPayload에 inviteId 저장
     *
     * @param request 초대 요청
     * @param principal 인증된 사용자 (방장)
     * @return 생성된 초대장 응답
     */
    @PostMapping
    @Operation(summary = "초대장 생성", description = "개인 채팅의 방장이 상대방을 단체 모임으로 초대합니다. 자동으로 INVITE_CARD 메시지가 생성되어 개인 채팅에 표시됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "초대장 생성 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (방장만 가능)"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 사용자 없음")
    })
    public ResponseEntity<ChatInviteResponse> createInvite(
            @RequestBody CreateChatInviteRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long inviterId = principal.id();

        // 초대장 생성
        ChatInvite invite = chatInviteService.createInvite(
                request.getPrivateChatRoomId(),
                inviterId,
                request.getInviteeId(),
                request.getTargetGroupPostId()
        );

        // INVITE_CARD 메시지 생성
        try {
            String cardPayload = objectMapper.writeValueAsString(
                    new InviteCardPayload(
                            invite.getId(),
                            invite.getInvitee().getId(),
                            request.getTargetGroupPostId(),
                            request.getTargetChatRoomId()
                    )
            );

            chatMessageService.saveMessage(
                    request.getPrivateChatRoomId(),
                    inviterId,
                    com.sobunsobun.backend.domain.ChatMessageType.INVITE_CARD,
                    null,
                    null,
                    cardPayload
            );

            log.info("INVITE_CARD 메시지 생성 - roomId: {}, inviteId: {}",
                    request.getPrivateChatRoomId(), invite.getId());
        } catch (Exception e) {
            log.error("INVITE_CARD 메시지 생성 실패", e);
        }

        ChatInviteResponse response = toChatInviteResponse(invite,
                request.getTargetGroupPostId(), request.getTargetChatRoomId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 받은 초대장 목록 조회
     *
     * @param principal 인증된 사용자
     * @return 대기 중인 초대장 목록
     */
    @GetMapping
    @Operation(summary = "받은 초대장 목록 조회", description = "현재 사용자가 받은 대기 중인 초대장 목록을 조회합니다. 7일 이내 생성된 초대장만 표시됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<List<ChatInviteResponse>> getPendingInvites(
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();
        List<ChatInvite> invites = chatInviteService.getPendingInvites(userId);

        List<ChatInviteResponse> responses = invites.stream()
                .map(invite -> {
                    // cardPayload에서 targetGroupPostId와 targetChatRoomId 추출 시도
                    ChatMessage message = chatMessageRepository
                            .findByRoomAndType(invite.getChatRoom().getId(),
                                    com.sobunsobun.backend.domain.ChatMessageType.INVITE_CARD)
                            .stream()
                            .filter(m -> m.getCardPayload() != null &&
                                   m.getCardPayload().contains("\"inviteId\":" + invite.getId()))
                            .findFirst()
                            .orElse(null);

                    Long targetGroupPostId = null;
                    Long targetChatRoomId = null;
                    if (message != null) {
                        try {
                            InviteCardPayload payload = objectMapper.readValue(
                                    message.getCardPayload(), InviteCardPayload.class
                            );
                            targetGroupPostId = payload.getTargetGroupPostId();
                            targetChatRoomId = payload.getTargetChatRoomId();
                        } catch (Exception e) {
                            log.warn("cardPayload 파싱 실패", e);
                        }
                    }

                    return toChatInviteResponse(invite, targetGroupPostId, targetChatRoomId);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 초대 수락
     *
     * @param id 초대장 ID
     * @param request 수락 요청
     * @param principal 인증된 사용자
     * @return 수락된 초대장
     */
    @PutMapping("/{id}/accept")
    @Operation(summary = "초대 수락", description = "받은 초대장을 수락합니다. 수락 시 해당 단체 모임의 채팅방에 자동으로 멤버가 추가되고, 입장 시스템 메시지가 전송됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수락 성공"),
            @ApiResponse(responseCode = "404", description = "초대장 없음"),
            @ApiResponse(responseCode = "410", description = "초대장 만료됨")
    })
    public ResponseEntity<ChatInviteResponse> acceptInvite(
            @PathVariable Long id,
            @RequestBody AcceptChatInviteRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();

        ChatInvite invite = chatInviteService.acceptInvite(
                id,
                userId,
                request.getTargetGroupChatRoomId()
        );

        // 시스템 메시지 생성 및 전송은 Service에서 처리 가능하도록 확장
        // 또는 여기서 직접 처리

        ChatInviteResponse response = toChatInviteResponse(invite, null, request.getTargetGroupChatRoomId());
        return ResponseEntity.ok(response);
    }

    /**
     * 초대 거절
     *
     * @param id 초대장 ID
     * @param principal 인증된 사용자
     * @return 거절된 초대장
     */
    @PutMapping("/{id}/decline")
    @Operation(summary = "초대 거절", description = "받은 초대장을 거절합니다. 거절된 초대장은 목록에서 제거됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "거절 성공"),
            @ApiResponse(responseCode = "404", description = "초대장 없음")
    })
    public ResponseEntity<ChatInviteResponse> declineInvite(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();

        ChatInvite invite = chatInviteService.declineInvite(id, userId);

        ChatInviteResponse response = toChatInviteResponse(invite, null, null);
        return ResponseEntity.ok(response);
    }

    /**
     * ChatInvite를 ChatInviteResponse로 변환
     */
    private ChatInviteResponse toChatInviteResponse(
            ChatInvite invite,
            Long targetGroupPostId,
            Long targetChatRoomId
    ) {
        return ChatInviteResponse.builder()
                .inviteId(invite.getId())
                .chatRoomId(invite.getChatRoom().getId())
                .inviterId(invite.getInviter().getId())
                .inviterName(invite.getInviter().getNickname())
                .inviterProfileImageUrl(invite.getInviter().getProfileImageUrl())
                .inviteeId(invite.getInvitee().getId())
                .status(invite.getStatus().name())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .targetGroupPostId(targetGroupPostId)
                .targetChatRoomId(targetChatRoomId)
                .build();
    }

    /**
     * INVITE_CARD 페이로드 DTO
     */
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class InviteCardPayload {
        private Long inviteId;
        private Long inviteeId;
        private Long targetGroupPostId;
        private Long targetChatRoomId;
    }
}
