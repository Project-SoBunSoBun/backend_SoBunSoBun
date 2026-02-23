package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatMemberStatus;
import com.sobunsobun.backend.domain.chat.ChatMessage;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import com.sobunsobun.backend.dto.chat.*;
import com.sobunsobun.backend.repository.chat.ChatMessageRepository;
import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import com.sobunsobun.backend.repository.chat.ChatRoomRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final ChatMessageService chatMessageService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

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

    // ====== 단체 채팅방 API ======

    /**
     * 단체 채팅방 생성/조회
     *
     * 공동구매 게시글에 연결된 단체 채팅방을 생성하거나 기존 채팅방을 조회합니다.
     * 동일 게시글에 이미 단체 채팅방이 있으면 기존 방을 반환합니다.
     */
    @Operation(
        summary = "단체 채팅방 생성/조회",
        description = "공동구매 게시글에 연결된 단체 채팅방을 생성하거나 기존 채팅방을 조회합니다"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "단체 채팅방 생성/조회 성공",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"success\", \"code\": 200, \"data\": {\"roomId\": 1, \"roomName\": \"떠나바 모임\", \"roomType\": \"GROUP\", \"groupPostId\": 5, \"memberCount\": 3, \"isNewRoom\": true}, \"message\": \"단체 채팅방 생성 성공\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 게시글",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"error\", \"code\": 404, \"error\": \"POST_NOT_FOUND\", \"message\": \"존재하지 않는 게시글입니다 (groupPostId: 5)\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청"
        )
    })
    @PostMapping("/rooms/group")
    public ResponseEntity<ApiResponse<CreateChatRoomResponse>> createGroupChatRoom(
            @RequestBody CreateGroupChatRoomRequest request,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("👥 [REST] 단체 채팅방 생성/조회 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - roomName: {}, groupPostId: {}, memberIds: {}",
                    request.getRoomName(), request.getGroupPostId(), request.getMemberIds());

            CreateChatRoomResponse response = chatRoomService.createOrGetGroupChatRoom(
                    userId,
                    request.getRoomName(),
                    request.getGroupPostId(),
                    request.getMemberIds()
            );

            log.info("✅ [REST] 단체 채팅방 API 완료 - roomId: {}, isNewRoom: {}",
                    response.getRoomId(), response.getIsNewRoom());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(response,
                    Boolean.TRUE.equals(response.getIsNewRoom()) ? "단체 채팅방 생성 성공" : "기존 단체 채팅방 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 단체 채팅방 API - 유효하지 않은 요청");
            log.warn("   - groupPostId: {}", request != null ? request.getGroupPostId() : "unknown");
            log.warn("   - errorMsg: {}", e.getMessage());

            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound("POST_NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 단체 채팅방 API 실패", e);
            log.error("   - groupPostId: {}", request != null ? request.getGroupPostId() : "unknown");
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("CREATE_GROUP_ROOM_FAILED", e.getMessage()));
        }
    }

    /**
     * 단체 채팅방 멤버 초대
     */
    @Operation(
        summary = "단체 채팅방 멤버 초대",
        description = "단체 채팅방에 새 멤버를 초대합니다. 채팅방 멤버만 다른 사용자를 초대할 수 있습니다."
    )
    @PostMapping("/rooms/{roomId}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> inviteMemberToGroupChat(
            @PathVariable("roomId") Long roomId,
            @PathVariable("targetUserId") Long targetUserId,
            Principal principal
    ) {
        try {
            log.info("➕ [REST] 단체 채팅 멤버 초대 - roomId: {}, targetUserId: {}", roomId, targetUserId);

            Long userId = extractUserIdFromPrincipal(principal);
            chatRoomService.addMemberToGroupChat(roomId, userId, targetUserId);

            log.info("✅ [REST] 멤버 초대 완료");
            return ResponseEntity.ok(ApiResponse.success(null, "멤버 초대 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 멤버 초대 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("INVITE_FAILED", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [REST] 멤버 초대 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("INVITE_FAILED", e.getMessage()));
        }
    }

    /**
     * 단체 채팅방 나가기
     */
    @Operation(
        summary = "단체 채팅방 나가기",
        description = "단체 채팅방에서 나갑니다. 나간 사용자는 더 이상 메시지를 받지 않습니다."
    )
    @DeleteMapping("/rooms/{roomId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveGroupChatRoom(
            @PathVariable("roomId") Long roomId,
            Principal principal
    ) {
        try {
            log.info("🚪 [REST] 단체 채팅 퇴장 - roomId: {}", roomId);

            Long userId = extractUserIdFromPrincipal(principal);
            chatRoomService.leaveGroupChatRoom(roomId, userId);

            log.info("✅ [REST] 채팅방 퇴장 완료");
            return ResponseEntity.ok(ApiResponse.success(null, "채팅방 퇴장 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 채팅방 퇴장 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("LEAVE_FAILED", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [REST] 채팅방 퇴장 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("LEAVE_FAILED", e.getMessage()));
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

    // ====== 채팅방 상세 정보 API ======

    /**
     * 채팅방 상세 정보 조회
     *
     * 개인(ONE_TO_ONE) / 단체(GROUP) 채팅방 모두 지원합니다.
     * - 개인: 상대방 유저 정보 (userId, nickname, profileImage) 포함
     * - 단체: 전체 멤버 목록 + 연결된 공동구매 게시글 정보 포함
     */
    @Operation(
        summary = "채팅방 상세 정보 조회",
        description = "채팅방의 상세 정보를 조회합니다. 개인 채팅방은 상대방 정보, 단체 채팅방은 멤버 목록과 게시글 정보를 포함합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "채팅방 상세 정보 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 채팅방"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님"
        )
    })
    @GetMapping("/rooms/{roomId}/detail")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoomDetail(
            @PathVariable("roomId") Long roomId,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("ℹ️ [REST] 채팅방 상세 조회 API 요청 - roomId: {}", roomId);

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);

            ChatRoomDetailResponse response = chatRoomService.getChatRoomDetail(roomId, userId);

            log.info("✅ [REST] 채팅방 상세 조회 완료 - roomType: {}, memberCount: {}",
                    response.getRoomType(), response.getMemberCount());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(response, "채팅방 상세 정보 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 채팅방 상세 조회 실패 - {}", e.getMessage());

            if (e.getMessage().contains("멤버가 아닙니다")) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("NOT_MEMBER", e.getMessage()));
            }
            return ResponseEntity.status(404)
                    .body(ApiResponse.notFound("ROOM_NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            log.error("❌ [REST] 채팅방 상세 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("GET_ROOM_DETAIL_FAILED", e.getMessage()));
        }
    }

    // ====== 메시지 관련 API ======

    /**
     * 채팅방 메시지 조회
     */
    @Operation(
        summary = "메시지 조회 (페이징)",
        description = "채팅방의 메시지 목록을 페이징하여 조회합니다. 최신 메시지부터 내림차순으로 정렬됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "메시지 조회 성공",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = """
                    {
                      "status": "success",
                      "code": 200,
                      "data": {
                        "content": [
                          {
                            "id": 123,
                            "roomId": 1,
                            "senderId": 456,
                            "userId": 456,
                            "senderName": "홍길동",
                            "nickname": "홍길동",
                            "senderProfileImageUrl": "/files/profile456.jpg",
                            "profileImage": "/files/profile456.jpg",
                            "type": "TEXT",
                            "content": "안녕하세요!",
                            "imageUrl": null,
                            "cardPayload": null,
                            "readCount": 1,
                            "createdAt": "2026-02-24T14:30:00",
                            "readByMe": true
                          },
                          {
                            "id": 122,
                            "roomId": 1,
                            "senderId": 123,
                            "userId": 123,
                            "senderName": "나",
                            "nickname": "나",
                            "senderProfileImageUrl": "/files/profile123.jpg",
                            "profileImage": "/files/profile123.jpg",
                            "type": "IMAGE",
                            "content": "사진 보내드려요",
                            "imageUrl": "/files/chat-img-abc123.jpg",
                            "cardPayload": null,
                            "readCount": 2,
                            "createdAt": "2026-02-24T14:25:00",
                            "readByMe": true
                          }
                        ],
                        "totalElements": 100,
                        "totalPages": 2,
                        "currentPage": 0,
                        "size": 50,
                        "first": true,
                        "last": false
                      },
                      "message": "메시지 조회 완료"
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"error\", \"code\": 403, \"error\": \"NOT_MEMBER\", \"message\": \"채팅방 멤버가 아닙니다\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    example = "{\"status\": \"error\", \"code\": 400, \"error\": \"GET_MESSAGES_FAILED\", \"message\": \"메시지 조회 실패\"}"
                )
            )
        )
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable("roomId") Long roomId,
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
                    .first(page == 0)
                    .last(page >= messages.getTotalPages() - 1)
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

    // ====== 이미지 업로드 API ======

    /**
     * 채팅 이미지 업로드
     *
     * FormData 방식으로 이미지 파일을 업로드하고 채팅 메시지로 저장합니다.
     * Content-Type: multipart/form-data
     *
     * @param image 이미지 파일 (jpg/png/webp, 최대 5MB)
     * @param chatId 채팅방 ID
     * @param message 메시지 내용 (선택)
     * @param principal 인증 사용자
     * @return 이미지 메시지 응답
     */
    @Operation(
            summary = "채팅 이미지 업로드",
            description = "FormData 방식으로 이미지 파일을 업로드합니다. Content-Type: multipart/form-data. 지원 형식: jpg/png/webp (최대 5MB)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "이미지 업로드 성공",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ChatImageMessageResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (파일 형식 또는 크기 초과)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님"
        )
    })
    @PostMapping(value = "/rooms/{chatId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatImageMessageResponse>> uploadChatImage(
            @PathVariable("chatId") Long chatId,
            @Parameter(description = "이미지 파일 (jpg/png/webp, 최대 5MB)", required = true)
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "메시지 내용 (선택)")
            @RequestParam(value = "message", required = false) String message,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("🖼️ [REST] 채팅 이미지 업로드 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - chatId: {}, imageSize: {} bytes, message: {}",
                    chatId, image != null ? image.getSize() : 0, message);

            // 1. 이미지 파일 검증
            if (image == null || image.isEmpty()) {
                log.warn("❌ 이미지 파일이 없음");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("IMAGE_REQUIRED", "이미지 파일이 필요합니다"));
            }

            // 2. 권한 체크 (채팅방 멤버 확인)
            log.debug("🔐 권한 체크 중... chatId: {}, userId: {}", chatId, userId);
            boolean isMember = chatMemberRepository.findMember(chatId, userId).isPresent();
            if (!isMember) {
                log.warn("❌ 권한 없음 - userId: {}는 chatId: {} 멤버가 아님", userId, chatId);
                return ResponseEntity.status(403)
                        .body(ApiResponse.forbidden("NOT_MEMBER", "채팅방 멤버가 아닙니다"));
            }
            log.info("✅ 권한 확인 완료 - 멤버임");

            // 3. 이미지 파일 저장
            log.debug("📤 이미지 파일 저장 중...");
            String imageUrl = fileStorageService.saveImage(image);
            log.info("✅ 이미지 저장 완료 - imageUrl: {}", imageUrl);

            // 4. 채팅 메시지 저장 (IMAGE 타입)
            log.debug("💬 채팅 메시지 저장 중...");
            MessageResponse savedMessage = chatMessageService.saveMessage(
                    chatId,
                    userId,
                    ChatMessageType.IMAGE,
                    message,  // 이미지와 함께 전송된 텍스트 메시지
                    imageUrl,
                    null  // cardPayload는 없음
            );
            log.info("✅ 채팅 메시지 저장 완료 - messageId: {}", savedMessage.getId());

            // 5. 사용자 정보 조회
            User sender = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            // 6. 응답 생성 (개선된 필드명 적용)
            ChatImageMessageResponse response = ChatImageMessageResponse.builder()
                    .id(savedMessage.getId())
                    .roomId(chatId)
                    .userId(userId)  // 신규 필드
                    .nickname(sender.getNickname())  // senderName -> nickname
                    .profileImage(sender.getProfileImageUrl())  // senderProfileImageUrl -> profileImage
                    .type("IMAGE")
                    .content(message)
                    .imageUrl(imageUrl)
                    .readCount(0)
                    .createdAt(savedMessage.getCreatedAt())  // timestamp -> createdAt (ISO 8601)
                    .readByMe(true)
                    .build();

            log.info("✅ [REST] 채팅 이미지 업로드 완료 - messageId: {}", savedMessage.getId());
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(response, "이미지 업로드 성공"));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 채팅 이미지 업로드 실패", e);
            log.error("   - chatId: {}, errorMsg: {}", chatId, e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("IMAGE_UPLOAD_FAILED", e.getMessage()));
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
                .userId(msg.getSender().getId())  // 신규 필드: userId
                .senderName(msg.getSender().getNickname())
                .nickname(msg.getSender().getNickname())  // 신규 필드: nickname
                .senderProfileImageUrl(msg.getSender().getProfileImageUrl())
                .profileImage(msg.getSender().getProfileImageUrl())  // 신규 필드: profileImage
                .type(msg.getType().toString())
                .content(msg.getContent())
                .imageUrl(msg.getImageUrl())
                .cardPayload(msg.getCardPayload())
                .readCount(msg.getReadCount())
                .createdAt(msg.getCreatedAt())  // ISO 8601 형식으로 자동 변환
                .readByMe(readByMe)
                .build();
    }

    /**
     * 채팅방 목록 조회 (개선 버전)
     *
     * iOS 클라이언트용 최적화 엔드포인트
     * 각 채팅방의 마지막 메시지와 안 읽은 메시지 개수를 포함
     *
     * API: GET /api/v1/chat/rooms/list
     * 응답: List<ChatRoomListResponseDto>
     * 정렬: lastMessageTime 기준 내림차순 (최신순)
     */
    @Operation(
            summary = "채팅방 목록 조회 (iOS용)",
            description = "사용자의 모든 채팅방을 조회합니다. 마지막 메시지와 안 읽은 메시지 개수를 포함하며, 최신 메시지 순으로 정렬됩니다."
    )
    @GetMapping("/rooms/list")
    public ResponseEntity<ApiResponse<List<ChatRoomListResponseDto>>> getChatRoomList(
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📋 [REST] 채팅방 목록 조회 API 요청");

            // 인증된 사용자 ID 추출
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);

            // ChatRoomService.getChatRoomList() 호출
            log.debug("🔄 ChatRoomService.getChatRoomList() 호출 중...");
            List<ChatRoomListResponseDto> chatRoomList = chatRoomService.getChatRoomList(userId);
            log.info("✅ 채팅방 목록 조회 완료 - roomCount: {}", chatRoomList.size());

            // 응답 반환
            log.info("✅ [REST] 채팅방 목록 API 완료");
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(chatRoomList, "채팅방 목록 조회 성공"));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 채팅방 목록 API 실패", e);
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("CHAT_ROOM_LIST_FAILED", e.getMessage()));
        }
    }

    /**
     * 1:1 채팅방 생성/조회 (개선 버전)
     *
     * 새로운 사용자와 1:1 채팅을 시작할 때 호출합니다.
     * 기존 1:1 채팅방이 있으면 그것을 반환하고,
     * 없으면 새로운 ONE_TO_ONE 타입의 채팅방을 생성합니다.
     *
     * API: POST /api/v1/chat/rooms
     * 요청: CreateOneToOneRoomRequest { targetUserId }
     * 응답: CreateOneToOneRoomResponse { roomId, otherUserName, otherUserProfileImageUrl, isNewRoom }
     */
    @Operation(
            summary = "1:1 채팅방 생성/조회",
            description = "새로운 사용자와의 1:1 채팅방을 생성하거나 기존 채팅방을 조회합니다"
    )
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<com.sobunsobun.backend.dto.chat.CreateOneToOneRoomResponse>> createOneToOneRoom(
            @RequestBody com.sobunsobun.backend.dto.chat.CreateOneToOneRoomRequest request,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📱 [REST] 1:1 채팅방 생성/조회 API 요청");

            Long myUserId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - myUserId: {}", myUserId);
            log.info("📝 요청 정보 - targetUserId: {}", request.getTargetUserId());

            log.debug("🔄 ChatRoomService.createOrGetOneToOneRoom() 호출 중...");
            var response = chatRoomService.createOrGetOneToOneRoom(myUserId, request.getTargetUserId());
            log.info("✅ 1:1 채팅방 생성/조회 완료 - roomId: {}, isNewRoom: {}",
                    response.getRoomId(), response.getIsNewRoom());

            log.info("✅ [REST] 1:1 채팅방 API 완료");
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(response, "1:1 채팅방 생성/조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 1:1 채팅방 API - 유효하지 않은 사용자 요청");
            log.warn("   - targetUserId: {}", request != null ? request.getTargetUserId() : "unknown");
            log.warn("   - errorMsg: {}", e.getMessage());

            return ResponseEntity.status(400)
                    .body(ApiResponse.badRequest("INVALID_USER", e.getMessage()));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 1:1 채팅방 API 실패", e);
            log.error("   - targetUserId: {}", request != null ? request.getTargetUserId() : "unknown");
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("CREATE_ONE_TO_ONE_ROOM_FAILED", e.getMessage()));
        }
    }

    /**
     * 과거 메시지 조회 (무한 스크롤)
     *
     * 커서 기반 페이징을 사용하여 채팅방의 과거 메시지를 조회합니다.
     * 모바일 앱의 무한 스크롤 기능을 지원합니다.
     *
     * API: GET /api/v1/chat/rooms/{roomId}/messages/cursor
     * 쿼리 파라미터: lastMessageId (커서, 처음엔 null), size (기본 20)
     * 응답: List<ChatMessageDto> (오름차순, 시간순)
     */
    @Operation(
            summary = "과거 메시지 조회 (무한 스크롤)",
            description = "채팅방의 과거 메시지를 커서 기반 페이징으로 조회합니다. 클라이언트 무한 스크롤 기능을 지원합니다."
    )
    @GetMapping("/rooms/{roomId}/messages/cursor")
    public ResponseEntity<ApiResponse<List<com.sobunsobun.backend.dto.chat.ChatMessageDto>>> getChatMessages(
            @PathVariable("roomId") Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📜 [REST] 과거 메시지 조회 API 요청");

            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ 인증 완료 - userId: {}", userId);
            log.info("📝 요청 정보 - roomId: {}, lastMessageId: {}, size: {}",
                    roomId, lastMessageId, size);

            log.debug("🔄 ChatMessageService.getChatMessages() 호출 중...");
            List<com.sobunsobun.backend.dto.chat.ChatMessageDto> messages = chatMessageService.getChatMessages(
                    roomId,
                    userId,
                    lastMessageId,
                    size
            );
            log.info("✅ 과거 메시지 조회 완료 - messageCount: {}", messages.size());

            log.info("✅ [REST] 과거 메시지 API 완료");
            log.info("═════════════════════════════════════════════════════════════");

            return ResponseEntity.ok(ApiResponse.success(messages, "과거 메시지 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ [REST] 과거 메시지 API - 권한 오류");
            log.warn("   - roomId: {}, userId: {}", roomId, principal.getName());
            log.warn("   - errorMsg: {}", e.getMessage());

            return ResponseEntity.status(403)
                    .body(ApiResponse.forbidden("ACCESS_DENIED", e.getMessage()));

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [REST] 과거 메시지 API 실패", e);
            log.error("   - roomId: {}, userId: {}", roomId, principal.getName());
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            return ResponseEntity.status(500)
                    .body(ApiResponse.serverError("GET_CHAT_MESSAGES_FAILED", e.getMessage()));
        }
    }
}
