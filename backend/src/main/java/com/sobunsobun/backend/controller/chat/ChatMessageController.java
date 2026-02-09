package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.application.chat.ChatRoomService;
import com.sobunsobun.backend.domain.ChatMessageType;
import com.sobunsobun.backend.dto.chat.ChatMessageResponse;
import com.sobunsobun.backend.dto.chat.MarkAsReadRequest;
import com.sobunsobun.backend.dto.chat.SendChatMessageRequest;
import com.sobunsobun.backend.dto.chat.UnreadUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket STOMP 메시지 처리 Controller
 *
 * STOMP 라우팅:
 * - /app/chat/send → /topic/rooms/{roomId}
 * - /app/chat/read → /user/{userId}/queue/private 및 /topic/rooms/{roomId}/read
 * - /app/chat/join → (내부 처리, 브로드캐스트 없음)
 * - /app/chat/leave → /topic/rooms/{roomId} (시스템 메시지)
 * - /app/chat/invite → (REST API로 이동 권장)
 *
 * 권한 검증:
 * - CONNECT 시: WebSocketAuthInterceptor가 JWT 검증
 * - SEND/SUBSCRIBE 시: Controller에서 room member 확인
 *
 * iOS 클라이언트 사용 흐름:
 * 1. WebSocket 연결: /ws/chat (Bearer token in header)
 * 2. SUBSCRIBE: /topic/rooms/{roomId}
 * 3. SEND: /app/chat/send (SendChatMessageRequest)
 * 4. READ: /app/chat/read (MarkAsReadRequest)
 * 5. DISCONNECT: (자동)
 */
@Slf4j
@Controller
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송
     *
     * 클라이언트 → /app/chat/send
     * 서버 → /topic/rooms/{roomId}
     *
     * @param request 메시지 전송 요청
     * @param principal 인증된 사용자 (Principal.name = userId)
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload SendChatMessageRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("🔥 STOMP sendMessage 메서드 호출됨!");
        log.info("📬 요청 정보 - roomId: {}, type: {}, content: {}",
                request.getRoomId(), request.getType(), request.getContent());

        // userId 추출
        Long userId = extractUserId(principal, headerAccessor);

        if (userId == null) {
            log.warn("❌ 메시지 전송 실패: 사용자 인증 정보 없음");
            return;
        }

        if (request.getRoomId() == null) {
            log.error("❌ 메시지 전송 실패: roomId가 null입니다");
            return;
        }

        try {
            log.info("💾 메시지 저장 시작... roomId: {}, userId: {}, type: {}",
                    request.getRoomId(), userId, request.getType());
            // ...existing code...
            ChatMessageResponse message = chatMessageService.saveMessage(
                    request.getRoomId(),
                    userId,
                    request.getType(),
                    request.getContent(),
                    request.getImageUrl(),
                    request.getCardPayload()
            );

            log.info("✅ 메시지 저장 완료 - messageId: {}, roomId: {}", message.getId(), request.getRoomId());

            // 채팅방의 모든 구독자에게 메시지 브로드캐스팅
            String destination = "/topic/rooms/" + request.getRoomId();
            log.info("📢 브로드캐스팅 시작 - destination: {}, message: {}", destination, message);

            try {
                messagingTemplate.convertAndSend(destination, message);
                log.info("✅ 메시지 브로드캐스트 완료 - roomId: {}, messageId: {}, destination: {}",
                        request.getRoomId(), message.getId(), destination);
            } catch (Exception broadcastException) {
                log.error("❌ 브로드캐스팅 중 오류 발생 - destination: {}, error: {}",
                        destination, broadcastException.getMessage(), broadcastException);
                throw broadcastException;
            }

        } catch (Exception e) {
            log.error("❌ 메시지 전송 실패 - roomId: {}, userId: {}, error: {}, stackTrace: {}",
                    request.getRoomId(), userId, e.getMessage(), e);
            // 클라이언트에 에러 응답
            sendErrorToUser(userId, "메시지 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 메시지 읽음 처리
     *
     * 클라이언트 → /app/chat/read
     * 서버 → /topic/rooms/{roomId}/read (모든 멤버에게)
     *
     * @param request 읽음 처리 요청
     * @param principal 인증된 사용자
     */
    @MessageMapping("/chat/read")
    public void markAsRead(
            @Payload MarkAsReadRequest request,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            log.warn("읽음 처리 실패: 사용자 인증 정보 없음");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        try {
            // 읽음 처리
            UnreadUpdatedEvent event = chatMessageService.markAsRead(
                    request.getRoomId(),
                    userId,
                    request.getLastReadMessageId()
            );

            // 채팅방의 모든 구독자에게 읽음 이벤트 브로드캐스팅
            // (다른 사용자의 읽음 상태 업데이트)
            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + request.getRoomId() + "/read",
                    event
            );

            // 개인 메시지: 현재 사용자의 미읽은 개수도 전송 (선택사항)
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/unread-count",
                    event
            );

            log.debug("읽음 처리 - roomId: {}, userId: {}, lastReadMessageId: {}",
                    request.getRoomId(), userId, request.getLastReadMessageId());

        } catch (Exception e) {
            log.error("읽음 처리 실패 - roomId: {}, userId: {}, error: {}",
                    request.getRoomId(), userId, e.getMessage());
        }
    }

    /**
     * 채팅방 입장 (선택사항)
     *
     * 클라이언트 → /app/chat/join/{roomId}
     * 서버 → /topic/rooms/{roomId} (시스템 메시지)
     *
     * iOS에서 SUBSCRIBE 시 입장 처리는 자동으로 처리될 수 있음
     * 명시적인 이벤트가 필요하면 이 엔드포인트 사용
     *
     * @param roomId 채팅방 ID
     * @param principal 인증된 사용자
     */
    @MessageMapping("/chat/join/{roomId}")
    public void joinRoom(
            @DestinationVariable Long roomId,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            log.warn("채팅방 입장 실패: 사용자 인증 정보 없음");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        try {
            // 권한 검증: 사용자가 멤버인지 확인
            var chatRoom = chatRoomService.getChatRoomDetail(roomId, userId);

            log.debug("채팅방 입장 - roomId: {}, userId: {}", roomId, userId);
            // 입장 이벤트는 필요시 시스템 메시지로 브로드캐스트 가능

        } catch (Exception e) {
            log.error("채팅방 입장 실패 - roomId: {}, userId: {}, error: {}",
                    roomId, userId, e.getMessage());
        }
    }

    /**
     * 채팅방 퇴장 (선택사항)
     *
     * 클라이언트 → /app/chat/leave/{roomId}
     * 서버 → /topic/rooms/{roomId} (시스템 메시지)
     *
     * @param roomId 채팅방 ID
     * @param principal 인증된 사용자
     */
    @MessageMapping("/chat/leave/{roomId}")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            Principal principal
    ) {
        if (principal == null || principal.getName() == null) {
            log.warn("채팅방 퇴장 실패: 사용자 인증 정보 없음");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        try {
            // 채팅방 퇴장 처리
            chatRoomService.leaveChatRoom(roomId, userId);

            log.debug("채팅방 퇴장 - roomId: {}, userId: {}", roomId, userId);
            // 퇴장 이벤트는 필요시 시스템 메시지로 브로드캐스트 가능

        } catch (Exception e) {
            log.error("채팅방 퇴장 실패 - roomId: {}, userId: {}, error: {}",
                    roomId, userId, e.getMessage());
        }
    }

    /**
     * 사용자에게 에러 메시지 전송 (선택사항)
     *
     * @param userId 사용자 ID
     * @param message 에러 메시지
     */
    private void sendErrorToUser(Long userId, String message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    message
            );
        } catch (Exception e) {
            log.warn("에러 메시지 전송 실패: {}", e.getMessage());
        }
    }

    /**
     * 채팅방의 메시지 조회
     *
     * REST API: GET /api/chat/messages/{roomId}
     *
     * 사용자가 채팅방에 입장할 때 기존 메시지를 조회합니다.
     * 최근 메시지 50개를 반환합니다.
     *
     * @param roomId 채팅방 ID
     * @return 메시지 목록
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId
    ) {
        log.info("📥 메시지 조회 요청 - roomId: {}", roomId);

        try {
            // ChatMessageService에 메서드 추가 필요
            // 임시로 빈 리스트 반환 (아래에서 service 메서드 추가)
            List<ChatMessageResponse> messages = chatMessageService.getMessagesByRoomId(roomId, 50);

            log.info("✅ 메시지 조회 완료 - roomId: {}, count: {}", roomId, messages.size());
            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            log.error("❌ 메시지 조회 실패 - roomId: {}, error: {}", roomId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Principal 또는 STOMP 세션에서 userId 추출
     *
     * 추출 순서:
     * 1. HTTP Principal
     * 2. STOMP 세션 속성 (CONNECT에서 저장)
     * 3. STOMP Authentication
     *
     * @param principal HTTP Principal (STOMP 전에 우선순위)
     * @param headerAccessor STOMP 헤더
     * @return userId (또는 null)
     */
    private Long extractUserId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        // 1. Principal에서 시도
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                log.warn("⚠️ Principal name이 숫자가 아님: {}", principal.getName());
            }
        }

        // 2. STOMP 세션 속성에서 시도 (CONNECT에서 저장된 값)
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object userIdObj = headerAccessor.getSessionAttributes().get("userId");
            if (userIdObj != null) {
                try {
                    return Long.parseLong(userIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("⚠️ STOMP 세션 속성 userId가 숫자가 아님: {}", userIdObj);
                }
            }
        }

        // 3. STOMP Authentication에서 시도
        if (headerAccessor != null && headerAccessor.getUser() != null) {
            Principal user = headerAccessor.getUser();
            if (user != null && user.getName() != null) {
                try {
                    return Long.parseLong(user.getName());
                } catch (NumberFormatException e) {
                    log.warn("⚠️ STOMP Authentication name이 숫자가 아님: {}", user.getName());
                }
            }
        }

        return null;
    }
}
