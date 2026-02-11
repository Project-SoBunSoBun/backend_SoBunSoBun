package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.chat.MessageResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.dto.chat.CreatePrivateChatRoomRequest;
import com.sobunsobun.backend.dto.chat.CreateGroupChatRoomRequest;
import com.sobunsobun.backend.dto.chat.CreateChatRoomResponse;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅 REST API
 *
 * WebSocket은 실시간 메시지용, REST는 보조용
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatRestController {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;

    /**
     * 채팅방 메시지 조회 (REST)
     *
     * WebSocket 연결 전에 기존 메시지를 로드할 때 사용
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);

            log.info("📥 메시지 목록 조회 - roomId: {}", roomId);

            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            Page<ChatMessage> messages = chatMessageRepository
                    .findByChatRoomIdOrderByCreatedAtDesc(roomId, pageable);

            // DTO 변환
            List<MessageResponse> responses = messages.getContent()
                    .stream()
                    .map(msg -> toMessageResponse(msg, roomId, userId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageImpl<>(
                    responses,
                    pageable,
                    messages.getTotalElements()
            ));

        } catch (Exception e) {
            log.error("❌ 메시지 조회 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 커서 기반 메시지 조회 (과거 메시지)
     */
    @GetMapping("/rooms/{roomId}/messages/before")
    public ResponseEntity<Page<MessageResponse>> getMessagesBefore(
            @PathVariable Long roomId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);

            LocalDateTime cursorTime = cursor != null ? cursor : LocalDateTime.now();
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

            Page<ChatMessage> messages = chatMessageRepository
                    .findMessagesBeforeCursor(roomId, cursorTime, pageable);

            List<MessageResponse> responses = messages.getContent()
                    .stream()
                    .map(msg -> toMessageResponse(msg, roomId, userId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageImpl<>(
                    responses,
                    pageable,
                    messages.getTotalElements()
            ));

        } catch (Exception e) {
            log.error("❌ 이전 메시지 조회 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 사용자의 채팅방 목록 조회 (unreadCount 포함)
     *
     * GET /api/v1/chat/rooms?page=0&size=20
     * 응답: 최신순 정렬된 채팅방 목록, 각 채팅방에 unreadCount 포함
     */
    @GetMapping("/rooms")
    public ResponseEntity<Page<ChatRoomResponse>> getChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);

            log.info("📋 채팅방 목록 조회 - userId: {}", userId);

            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            Page<ChatRoom> chatRooms = chatRoomRepository.findUserChatRooms(userId, pageable);

            // DTO 변환 (unreadCount 포함)
            List<ChatRoomResponse> responses = chatRooms.getContent()
                    .stream()
                    .map(room -> {
                        long unreadCount = chatMemberRepository.countUnreadMessages(room.getId(), userId);
                        return ChatRoomResponse.builder()
                                .id(room.getId())
                                .name(room.getName() != null ? room.getName() : "")
                                .roomType(room.getRoomType().toString())
                                .ownerId(room.getOwner() != null ? room.getOwner().getId() : 0L)
                                .memberCount(room.getMembers().size())
                                .unreadCount(unreadCount)
                                .lastMessagePreview(room.getLastMessagePreview() != null ? room.getLastMessagePreview() : "")
                                .lastMessageAt(room.getLastMessageAt())
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new PageImpl<>(
                    responses,
                    pageable,
                    chatRooms.getTotalElements()
            ));

        } catch (Exception e) {
            log.error("❌ 채팅방 목록 조회 오류: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 채팅방 멤버 목록 조회
     */
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<?> getRoomMembers(
            @PathVariable Long roomId,
            Principal principal
    ) {
        try {
            log.info("👥 멤버 목록 조회 - roomId: {}", roomId);
            // TODO: ChatRoomService 연동
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ 멤버 조회 오류: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 개인 채팅방 생성
     */
    @PostMapping("/rooms/private")
    public ResponseEntity<CreateChatRoomResponse> createPrivateChatRoom(
            @RequestBody CreatePrivateChatRoomRequest request,
            Principal principal
    ) {
        try {
            Long userId = extractUserIdFromPrincipal(principal);

            log.info("🔒 개인 채팅방 생성 - userId: {}, otherUserId: {}", userId, request.getOtherUserId());

            ChatRoom chatRoom = chatRoomService.getOrCreatePrivateChatRoom(userId, request.getOtherUserId());

            CreateChatRoomResponse response = CreateChatRoomResponse.builder()
                    .roomId(chatRoom.getId())
                    .roomName(chatRoom.getName())
                    .roomType(chatRoom.getRoomType().toString())
                    .message("✅ 개인 채팅방 생성/조회 성공")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 채팅방 생성 오류: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 단체 채팅방 생성
     */
    @PostMapping("/rooms/group")
    public ResponseEntity<CreateChatRoomResponse> createGroupChatRoom(
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

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 단체 채팅방 생성 오류: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Principal에서 userId 추출 (JwtUserPrincipal 사용)
     */
    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Principal is null");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal) {
                JwtUserPrincipal jwtPrincipal = (JwtUserPrincipal) auth.getPrincipal();
                return jwtPrincipal.id();
            }
        } catch (Exception e) {
            log.warn("⚠️ JwtUserPrincipal 캐스팅 실패: {}", e.getMessage());
        }

        // 폴백: 문자열 파싱
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Cannot parse userId from principal: " + principal.getName());
        }
    }

    private MessageResponse toMessageResponse(ChatMessage message, Long roomId, Long requesterId) {
        ChatMember member = chatMemberRepository.findMember(roomId, requesterId)
                .orElse(null);

        boolean readByMe = member != null &&
                member.getLastReadMessageId() != null &&
                member.getLastReadMessageId() >= message.getId();

        return MessageResponse.builder()
                .id(message.getId())
                .roomId(roomId)
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getNickname())
                .senderProfileImageUrl(message.getSender().getProfileImageUrl())
                .type(message.getType())
                .content(message.getContent())
                .imageUrl(message.getImageUrl())
                .cardPayload(message.getCardPayload())
                .readCount(message.getReadCount())
                .createdAt(message.getCreatedAt())
                .readByMe(readByMe)
                .build();
    }
}

