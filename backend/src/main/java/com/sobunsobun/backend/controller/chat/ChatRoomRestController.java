package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageQueryService;
import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomListItemResponse;
import com.sobunsobun.backend.dto.chat.ChatRoomResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 채팅 REST API Controller
 *
 * 엔드포인트:
 * - GET /api/v1/chat/rooms - 채팅방 목록
 * - GET /api/v1/chat/rooms/{id} - 채팅방 상세
 * - GET /api/v1/chat/rooms/{id}/messages - 메시지 목록 (페이징)
 * - POST /api/v1/chat/rooms - 개인 채팅방 생성 (또는 조회)
 * - DELETE /api/v1/chat/rooms/{id} - 채팅방 나가기
 *
 * WebSocket과 REST의 역할 분담:
 * - WebSocket: 실시간 메시지 전송/수신, 읽음 처리
 * - REST: 목록/상세 조회, 초대 관리, 이미지 업로드 등
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "채팅방 관리", description = "채팅방 목록, 상세 조회, 메시지 조회, 개인 채팅 생성 등")
@RequiredArgsConstructor
public class ChatRoomRestController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageQueryService chatMessageQueryService;

    /**
     * 채팅방 목록 조회
     *
     * 페이징: 최신순 정렬
     *
     * @param principal 인증된 사용자
     * @param pageable 페이징 정보 (기본: 20개, 최신순)
     * @return 채팅방 목록
     */
    @GetMapping("/rooms")
    @Operation(summary = "채팅방 목록 조회", description = "현재 사용자의 모든 채팅방 목록을 최신순으로 조회합니다. 개인/단체 채팅 모두 포함되며, 읽지 않은 메시지 개수도 함께 제공됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Page<ChatRoomListItemResponse>> getChatRooms(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = principal.id();
        Page<ChatRoomListItemResponse> rooms = chatRoomService.getUserChatRooms(userId, pageable);
        return ResponseEntity.ok(rooms);
    }

    /**
     * 채팅방 상세 정보 조회
     *
     * @param id 채팅방 ID
     * @param principal 인증된 사용자
     * @return 채팅방 상세 정보
     */
    @GetMapping("/rooms/{id}")
    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방의 상세 정보를 조회합니다. 채팅방 이름, 멤버 수, 마지막 메시지 등이 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ResponseEntity<ChatRoomResponse> getChatRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();
        ChatRoomResponse room = chatRoomService.getChatRoomDetail(id, userId);
        return ResponseEntity.ok(room);
    }

    /**
     * 메시지 목록 조회 (페이징, 최신��)
     *
     * GET /api/v1/chat/rooms/{roomId}/messages?page=0&size=30
     *
     * @param roomId 채팅방 ID
     * @param principal 인증된 사용자
     * @param pageable 페이징 정보
     * @return 메시지 목록 페이지
     */
    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "메시지 목록 조회", description = "채팅방의 메시지 목록을 페이징으로 조회합니다. 최신 메시지부터 역순으로 정렬되며, 각 메시지의 발신자 정보가 포함됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = principal.id();
        Page<ChatMessageResponse> messages = chatMessageQueryService.getMessages(roomId, userId, pageable);
        return ResponseEntity.ok(messages);
    }


    /**
     * 커서 기반 메시지 조회 (특정 시점 이전 메시지)
     *
     * GET /api/v1/chat/rooms/{id}/messages/before?cursor=2025-01-27T10:30:00&page=0&size=30
     *
     * @param id 채팅방 ID
     * @param cursor 기준 시간 (이전 메시지 조회)
     * @param principal 인증된 사용자
     * @param pageable 페이징 정보
     * @return 메시지 목록 페이지
     */
    @GetMapping("/rooms/{id}/messages/before")
    @Operation(summary = "이전 메시지 조회 (커서 기반)", description = "특정 시간 이전의 메시지를 커서 기반으로 조회합니다. 무한 스크롤 구현에 유용합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<Page<ChatMessageResponse>> getMessagesBefore(
            @PathVariable(name = "id") Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursor,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = principal.id();
        Page<ChatMessageResponse> messages = chatMessageQueryService.getMessagesBefore(roomId, userId, cursor, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 개인 채팅방 생성 또�� 조회
     *
     * 1:1 채팅 시작
     *
     * @param otherUserId 상대방 사용자 ID (Request Body: {"otherUserId": 123})
     * @param principal 인증된 사용자
     * @return 생성되거나 조회된 채팅방
     */
    @PostMapping("/rooms/private")
    @Operation(summary = "개인 채팅방 생성 또는 조회", description = "다른 사용자와 1:1 개인 채팅방을 시작합니다. 이미 존재하는 채팅방이면 조회만 수행합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (기존 채팅방)"),
            @ApiResponse(responseCode = "201", description = "생성 성공 (새로운 채팅방)"),
            @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    public ResponseEntity<ChatRoomResponse> createOrGetPrivateChatRoom(
            @RequestBody CreatePrivateChatRoomRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();
        var chatRoom = chatRoomService.createOrGetPrivateChatRoom(userId, request.getOtherUserId());
        ChatRoomResponse response = chatRoomService.getChatRoomDetail(chatRoom.getId(), userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 나가기
     *
     * @param id 채팅방 ID
     * @param principal 인증된 사용자
     * @return 성공 응답
     */
    @DeleteMapping("/rooms/{id}")
    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다. 마지막 멤버가 나가면 채팅방이 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "나가기 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    public ResponseEntity<Void> leaveChatRoom(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        Long userId = principal.id();
        chatRoomService.leaveChatRoom(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 간단한 Request DTO
     */
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreatePrivateChatRoomRequest {
        private Long otherUserId;
    }
}
