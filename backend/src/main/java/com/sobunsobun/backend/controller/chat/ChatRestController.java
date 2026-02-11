package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatRoomService;
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
    @Operation(summary = "개인 채팅방 생성/조회", description = "상대방과의 개인 채팅방을 생성하거나 기존 채팅방을 조회합니다")
    @PostMapping("/rooms/private")
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createPrivateChatRoom(
            @RequestBody CreatePrivateChatRoomRequest request,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("🔒 개인 채팅방 생성/조회 - userId: {}, otherUserId: {}", userId, request.getOtherUserId());

            ChatRoom chatRoom = chatRoomService.getOrCreatePrivateChatRoom(userId, request.getOtherUserId());

            CreateChatRoomResponse response = CreateChatRoomResponse.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .roomType(chatRoom.getRoomType().toString())
                    .message("✅ 개인 채팅방 생성/조회 성공")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "채팅방 생성/조회 완료"));

        } catch (Exception e) {
            log.error("❌ 개인 채팅방 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("CREATE_PRIVATE_ROOM_FAILED", e.getMessage()));
        }
    }

    /**
     * 단체 채팅방 생성
     */
    @Operation(summary = "단체 채팅방 생성", description = "새로운 단체 채팅방을 생성합니다")
    @PostMapping("/rooms/group")
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createGroupChatRoom(
            @RequestBody CreateGroupChatRoomRequest request,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("👥 단체 채팅방 생성 - roomName: {}, groupPostId: {}", request.getRoomName(), request.getGroupPostId());

            ChatRoom chatRoom = chatRoomService.createGroupChatRoom(request.getRoomName(), userId, request.getGroupPostId());

            CreateChatRoomResponse response = CreateChatRoomResponse.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .roomType(chatRoom.getRoomType().toString())
                    .message("✅ 단체 채팅방 생성 성공")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "단체 채팅방 생성 완료"));

        } catch (Exception e) {
            log.error("❌ 단체 채팅방 생성 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("CREATE_GROUP_ROOM_FAILED", e.getMessage()));
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
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("📋 채팅방 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<ChatRoom> chatRooms = chatRoomRepository.findUserChatRooms(userId, pageable);

            List<ChatRoomResponse> responses = chatRooms.getContent()
                    .stream()
                    .map(room -> {
                        long unreadCount = chatMemberRepository.countUnreadMessages(room.getId(), userId);
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

            PageResponse<ChatRoomResponse> pageResponse = PageResponse.<ChatRoomResponse>builder()
                    .content(responses)
                    .totalElements(chatRooms.getTotalElements())
                    .totalPages(chatRooms.getTotalPages())
                    .currentPage(page)
                    .size(size)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse, "채팅방 목록 조회 완료"));

        } catch (Exception e) {
            log.error("❌ 채팅방 목록 조회 실패", e);
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
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("📥 메시지 조회 - roomId: {}, userId: {}", roomId, userId);

            // 권한 체크
            boolean isMember = chatMemberRepository.findMember(roomId, userId).isPresent();
            if (!isMember) {
                log.warn("❌ 권한 없음 - roomId: {}, userId: {}", roomId, userId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("NOT_MEMBER", "채팅방 멤버가 아닙니다"));
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);

            List<MessageResponse> responses = messages.getContent()
                    .stream()
                    .map(msg -> toMessageResponse(msg, userId))
                    .collect(Collectors.toList());

            PageResponse<MessageResponse> pageResponse = PageResponse.<MessageResponse>builder()
                    .content(responses)
                    .totalElements(messages.getTotalElements())
                    .totalPages(messages.getTotalPages())
                    .currentPage(page)
                    .size(size)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse, "메시지 조회 완료"));

        } catch (Exception e) {
            log.error("❌ 메시지 조회 실패", e);
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
