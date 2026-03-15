package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.infrastructure.redis.ChatRedisService;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.chat.MessageResponse;
import com.sobunsobun.backend.dto.chat.PageResponse;
import com.sobunsobun.backend.dto.chat.SendMessageRequest;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 채팅 메시지 REST API 컨트롤러
 *
 * 엔드포인트:
 * - POST   /api/messages              메시지 전송
 * - GET    /api/messages/{roomId}     메시지 목록 조회 (페이징)
 * - PATCH  /api/messages/{id}/read   읽음 처리
 */
@Slf4j
@Tag(name = "Chat - 메시지", description = "채팅 메시지 REST API")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class ChatMessageRestController {

    private final ChatMessageService chatMessageService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRedisService chatRedisService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/messages  —  메시지 전송
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 메시지 전송
     *
     * userId는 JWT 토큰에서 추출합니다.
     * settlementId가 있으면 cardPayload JSON으로 변환하여 저장합니다.
     * 저장 완료 후 Redis Pub/Sub을 통해 채팅방 구독자에게 실시간 브로드캐스트됩니다.
     *
     * createdAt은 서버 시간(KST, Asia/Seoul) 기준으로 자동 생성됩니다.
     * 응답의 createdAt 형식: "2026-02-26T15:00:00+09:00" (ISO 8601)
     */
    @Operation(
        summary = "메시지 전송",
        description = """
            채팅방에 메시지를 전송합니다.
            - userId는 JWT 토큰에서 자동 추출됩니다.
            - createdAt은 서버 시간(KST) 기준으로 자동 생성됩니다.
            - settlementId 입력 시 정산 카드 메시지로 저장됩니다.
            - 저장 후 Redis Pub/Sub → STOMP 구독자에게 실시간 전송됩니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "메시지 전송 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "잘못된 요청 (필드 검증 실패)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "채팅방 멤버가 아닌 경우"
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        log.info("📤 [메시지 전송 요청] userId: {}, roomId: {}", userId, request.getGroupChatRoomId());

        // settlementId가 있으면 SETTLEMENT_CARD, 없으면 TEXT (content 필수)
        String cardPayload = null;
        ChatMessageType type = ChatMessageType.TEXT;
        if (request.getSettlementId() != null && !request.getSettlementId().isBlank()) {
            // 정산서는 방장(owner)만 발송 가능
            ChatRoom chatRoom = chatRoomRepository.findById(request.getGroupChatRoomId())
                    .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!chatRoom.isOwner(userId)) {
                throw new ChatException(ErrorCode.CHAT_NOT_OWNER);
            }
            if (chatMessageRepository.existsByChatRoomIdAndType(request.getGroupChatRoomId(), ChatMessageType.SETTLEMENT_CARD)) {
                throw new ChatException(ErrorCode.CHAT_SETTLEMENT_ALREADY_SENT);
            }
            cardPayload = "{\"settlementId\":" + request.getSettlementId() + "}";
            type = ChatMessageType.SETTLEMENT_CARD;
        } else if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ChatException(ErrorCode.INVALID_REQUEST);
        }

        MessageResponse response = chatMessageService.saveMessage(
                request.getGroupChatRoomId(),
                userId,
                type,
                request.getContent(),
                null,
                cardPayload
        );

        log.info("✅ [메시지 전송 완료] messageId: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/messages/{groupChatRoomId}  —  메시지 목록 조회 (페이징)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 메시지 목록 조회 (오프셋 페이징)
     *
     * - 최신순 정렬 (createdAt DESC)
     * - 기본: page=0, size=20
     * - readByMe는 요청 사용자의 ChatMember.lastReadAt 기준으로 계산
     */
    @Operation(
        summary = "메시지 목록 조회",
        description = """
            특정 채팅방의 메시지 내역을 최신순으로 반환합니다.
            - 오프셋 기반 페이징 (page, size 쿼리 파라미터)
            - readByMe는 요청 유저의 마지막 읽은 시각 기준으로 계산됩니다.
            - createdAt 형식: ISO 8601 ("2026-02-26T15:00:00+09:00")
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "채팅방 멤버가 아닌 경우"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "채팅방이 존재하지 않는 경우"
        )
    })
    @GetMapping("/{groupChatRoomId}")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @Parameter(description = "그룹 채팅방 ID", example = "1")
            @PathVariable Long groupChatRoomId,

            @Parameter(description = "페이지 번호 (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,

            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        log.info("📋 [메시지 목록 조회] roomId: {}, userId: {}, page: {}, size: {}", groupChatRoomId, userId, page, size);

        // 채팅방 멤버 검증
        if (!chatMemberRepository.isActiveMember(groupChatRoomId, userId)) {
            log.warn("❌ [메시지 목록 조회 실패] 접근 권한 없음 - roomId: {}, userId: {}", groupChatRoomId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("FORBIDDEN", "채팅방에 접근 권한이 없습니다."));
        }

        // 첫 페이지 진입 시 unread count 초기화 (채팅방 입장으로 간주)
        if (page == 0) {
            chatRedisService.enterRoom(userId, groupChatRoomId);
        }

        // 요청 유저의 lastReadAt 조회 (readByMe 판단 기준)
        var memberOpt = chatMemberRepository.findMember(groupChatRoomId, userId);
        var lastReadAt = memberOpt.map(m -> m.getLastReadAt()).orElse(null);

        // 최신순 페이징 조회
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.sobunsobun.backend.domain.chat.ChatMessage> messagePage =
                chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(groupChatRoomId, pageable);

        // 엔티티 → DTO 변환
        List<MessageResponse> content = messagePage.getContent().stream()
                .map(msg -> {
                    boolean readByMe = msg.getSender() != null
                            && msg.getSender().getId().equals(userId)  // 내가 보낸 메시지는 항상 읽음
                            || (lastReadAt != null && !lastReadAt.isBefore(msg.getCreatedAt()));

                    String senderName = msg.getSender() != null ? msg.getSender().getNickname() : null;
                    String profileImage = msg.getSender() != null ? msg.getSender().getProfileImageUrl() : null;
                    Long senderId = msg.getSender() != null ? msg.getSender().getId() : null;

                    return MessageResponse.builder()
                            .id(msg.getId())
                            .roomId(groupChatRoomId)
                            .senderId(senderId)
                            .userId(senderId)
                            .senderName(senderName)
                            .nickname(senderName)
                            .senderProfileImageUrl(profileImage)
                            .profileImage(profileImage)
                            .type(msg.getType().toString())
                            .content(msg.getContent())
                            .imageUrl(msg.getImageUrl())
                            .cardPayload(msg.getCardPayload())
                            .readCount(msg.getReadCount())
                            .createdAt(msg.getCreatedAt())
                            .readByMe(readByMe)
                            .settlementId(extractSettlementId(msg))
                            .groupChatRoomId(groupChatRoomId.intValue())
                            .build();
                })
                .toList();

        PageResponse<MessageResponse> pageResponse = PageResponse.<MessageResponse>builder()
                .content(content)
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .currentPage(page)
                .size(size)
                .build();

        log.info("✅ [메시지 목록 조회 완료] roomId: {}, count: {}", groupChatRoomId, content.size());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PATCH /api/messages/{id}/read  —  읽음 처리
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 특정 메시지 읽음 처리
     *
     * - readCount를 1 증가 (벌크 UPDATE, race condition 안전)
     * - ChatMember.lastReadAt을 해당 메시지의 createdAt으로 업데이트
     * - 응답의 readByMe는 true로 반환
     */
    @Operation(
        summary = "메시지 읽음 처리",
        description = """
            특정 메시지를 읽음 처리합니다.
            - readCount가 1 증가합니다.
            - 요청 유저의 마지막 읽은 시각(lastReadAt)이 해당 메시지의 createdAt으로 업데이트됩니다.
            - 응답의 readByMe는 true입니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "읽음 처리 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "채팅방 멤버가 아닌 경우"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "메시지가 존재하지 않는 경우"
        )
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<MessageResponse>> readMessage(
            @Parameter(description = "메시지 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            Authentication authentication
    ) {
        Long userId = extractUserId(authentication);
        log.info("👁 [읽음 처리 요청] messageId: {}, userId: {}", id, userId);

        MessageResponse response = chatMessageService.readMessage(id, userId);

        log.info("✅ [읽음 처리 완료] messageId: {}, readCount: {}", id, response.getReadCount());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 공통 유틸
    // ──────────────────────────────────────────────────────────────────────────

    private Long extractUserId(Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        return principal.id();
    }

    private Integer extractSettlementId(com.sobunsobun.backend.domain.chat.ChatMessage message) {
        if (message.getCardPayload() == null || message.getCardPayload().isBlank()) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(message.getCardPayload());
            if (node.has("settlementId") && !node.get("settlementId").isNull()) {
                return node.get("settlementId").asInt();
            }
        } catch (Exception e) {
            log.debug("settlementId 추출 실패: {}", e.getMessage());
        }
        return null;
    }
}
