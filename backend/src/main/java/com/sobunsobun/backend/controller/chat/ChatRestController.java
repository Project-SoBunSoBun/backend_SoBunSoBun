package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatMemberStatus;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.chat.*;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 REST API Controller
 *
 * WebSocket(STOMP)은 실시간 메시지 처리용
 * REST API는 채팅방 관리, 메시지 조회 등 보조용
 */
@Slf4j
@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatRestController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;

    // ====== 채팅방 관련 API ======

    /**
     * 개인 채팅방 생성/조회
     */
    @Operation(
        summary = "개인 채팅방 생성/조회",
        description = "상대방과의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "채팅방 생성/조회 성공",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"success\", \"code\": 200, \"data\": {\"roomId\": 1, \"roomName\": \"상대방이름\", \"roomType\": \"PRIVATE\"}, \"message\": \"채팅방 생성/조회 완료\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 사용자",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"error\", \"code\": 404, \"error\": \"USER_NOT_FOUND\", \"message\": \"존재하지 않는 사용자입니다 (userId: 4)\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"error\", \"code\": 400, \"error\": \"CREATE_PRIVATE_ROOM_FAILED\", \"message\": \"채팅방 생성 중 오류 발생\"}"
                )
            )
        )
    })
    @PostMapping("/rooms/private")
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createPrivateChatRoom(
            @RequestBody CreatePrivateChatRoomRequest request,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📱 [REST] 개인 채팅방 생성/조회 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - otherUserId: {}", request.getOtherUserId());

            log.debug("🔄 ChatRoomService.getOrCreatePrivateChatRoom() 호출 중...");
            ChatRoom chatRoom = chatRoomService.getOrCreatePrivateChatRoom(userId, request.getOtherUserId());
            log.info("✅ 채팅방 반환됨 - roomId: {}", chatRoom.getId());

            CreateChatRoomResponse response = CreateChatRoomResponse.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .roomType(chatRoom.getRoomType().toString())
                    .message("✅ 개인 채팅방 생성/조회 성공")
                    .build();

            log.info("✅ [REST] 개인 채팅방 API 완료 - roomId: {}", chatRoom.getId());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(response, "채팅방 생성/조회 완료"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 개인 채팅방 API - 유효하지 않은 사용자 요청");
            log.warn("   - otherUserId: {}", request != null ? request.getOtherUserId() : "unknown");
            log.warn("   - errorMsg: {}", e.getMessage());

            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound("USER_NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 개인 채팅방 API 실패", e);
            log.error("   - otherUserId: {}", request != null ? request.getOtherUserId() : "unknown");
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("CREATE_PRIVATE_ROOM_FAILED", e.getMessage()));
        }
    }


    /**
     * 채팅방 목록 조회
     */
    @Operation(summary = "채팅방 목록 조회", description = "사용자의 모든 채팅방 목록을 조회합니다 (unreadCount 포함)")
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<PageResponse<ChatRoomResponse>>> getChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📋 [REST] 채팅방 목록 조회 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - page: {}, size: {}", page, size);

            log.debug("🔄 ChatRoomRepository.findUserChatRooms() 조회 중...");
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatRoom> chatRooms = chatRoomRepository.findUserChatRooms(userId, pageable);

            log.info("✅ DB 조회 완료 - totalElements: {}, totalPages: {}",
                    chatRooms.getTotalElements(), chatRooms.getTotalPages());

            log.debug("🔄 채팅방 목록 변환 중...");
            List<ChatRoomResponse> responses = chatRooms.getContent()
                    .stream()
                    .map(room -> {
                        long unreadCount = chatMemberRepository.countUnreadMessages(room.getId(), userId);
                        log.debug("  - roomId: {}, roomName: {}, unreadCount: {}",
                                room.getId(), room.getName(), unreadCount);

                        return ChatRoomResponse.builder()
                                .id(room.getId())
                                .name(room.getName())
                                .roomType(room.getRoomType().toString())
                                .memberCount(room.getMembers().size())
                                .unreadCount(unreadCount)
                                .lastMessagePreview(room.getLastMessagePreview())
                                .lastMessageAt(room.getLastMessageAt())
                                .ownerId(room.getOwner() != null ? room.getOwner().getId() : null)
                                .build();
                    })
                    .collect(Collectors.toList());
            log.info("✅ 채팅방 목록 변환 완료 - count: {}", responses.size());

            PageResponse<ChatRoomResponse> pageResponse = PageResponse.<ChatRoomResponse>builder()
                    .content(responses)
                    .totalElements((long) responses.size())
                    .totalPages((responses.size() + size - 1) / size)
                    .currentPage(page)
                    .size(size)
                    .build();

            log.info("✅ [REST] 채팅방 목록 조회 완료 - count: {}, totalElements: {}",
                    responses.size(), responses.size());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(pageResponse, "채팅방 목록 조회 완료"));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 채팅방 목록 조회 실패", e);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("GET_ROOMS_FAILED", e.getMessage()));
        }
    }

    // ====== 메시지 관련 API ======

    /**
     * 채팅방 메시지 조회
     */
    @Operation(summary = "메시지 조회", description = "채팅방의 메시지 목록을 조회합니다")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📥 [REST] 메시지 조회 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - roomId: {}, page: {}, size: {}", roomId, page, size);

            // 권한 체크
            log.debug("🔐 권한 체크 중... roomId: {}, userId: {}", roomId, userId);
            boolean isMember = chatMemberRepository.findMember(roomId, userId).isPresent();
            if (!isMember) {
                log.warn("❌ 권한 없음 - userId: {}는 roomId: {} 멤버가 아님", userId, roomId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("NOT_MEMBER", "채팅방 멤버가 아닙니다"));
            }
            log.info("✅ 권한 확인 완료 - 멤버임");

            log.debug("🔄 ChatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc() 조회 중...");
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);
            log.info("✅ DB 조회 완료 - totalElements: {}, totalPages: {}",
                    messages.getTotalElements(), messages.getTotalPages());

            log.debug("🔄 메시지 목록 변환 중...");
            List<MessageResponse> responses = messages.getContent()
                    .stream()
                    .map(msg -> {
                        log.debug("  - messageId: {}, type: {}, contentLength: {}",
                                msg.getId(), msg.getType(), msg.getContent() != null ? msg.getContent().length() : 0);
                        return toMessageResponse(msg, userId);
                    })
                    .collect(Collectors.toList());
            log.info("✅ 메시지 목록 변환 완료 - count: {}", responses.size());

            PageResponse<MessageResponse> pageResponse = PageResponse.<MessageResponse>builder()
                    .content(responses)
                    .totalElements(messages.getTotalElements())
                    .totalPages(messages.getTotalPages())
                    .currentPage(page)
                    .size(size)
                    .build();

            log.info("✅ [REST] 메시지 조회 완료 - count: {}, totalElements: {}",
                    responses.size(), messages.getTotalElements());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(pageResponse, "메시지 조회 완료"));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 메시지 조회 실패", e);
            log.error("   - roomId: {}, errorMsg: {}", roomId, e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("GET_MESSAGES_FAILED", e.getMessage()));
        }
    }

    // ====== 유틸리티 메서드 ======

    /**
     * Principal에서 userId 추출
     * JwtUserPrincipal에서 직접 추출하므로 파싱 오류 없음
     */
    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Principal is null");
        }

        try {
            // SecurityContext에서 Authentication 조회
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                throw new RuntimeException("Authentication not found in SecurityContext");
            }

            // JwtUserPrincipal에서 직접 추출
            Object principalObj = auth.getPrincipal();
            if (principalObj instanceof com.sobunsobun.backend.security.JwtUserPrincipal) {
                Long userId = ((com.sobunsobun.backend.security.JwtUserPrincipal) principalObj).id();
                log.debug("✅ userId 추출 성공: {}", userId);
                return userId;
            }

            log.error("❌ Principal이 JwtUserPrincipal이 아님: {}", principalObj.getClass().getName());
            throw new RuntimeException("Invalid principal type: " + principalObj.getClass().getName());

        } catch (Exception e) {
            log.error("❌ userId 추출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("사용자 정보를 추출할 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * ChatMessage를 MessageResponse로 변환
     */
    private MessageResponse toMessageResponse(ChatMessage msg, Long userId) {
        // 간단한 읽음 처리: 자신의 메시지이거나 readCount > 0이면 읽음
        boolean readByMe = msg.getSender().getId().equals(userId) || (msg.getReadCount() != null && msg.getReadCount() > 0);

        return MessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getChatRoom().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getNickname())
                .senderProfileImageUrl(msg.getSender().getProfileImageUrl())
                .type(msg.getType().toString())
                .content(msg.getContent())
                .imageUrl(msg.getImageUrl())
                .cardPayload(msg.getCardPayload())
                .readCount(msg.getReadCount())
                .createdAt(msg.getCreatedAt())
                .readByMe(readByMe)
                .build();
    }
}
