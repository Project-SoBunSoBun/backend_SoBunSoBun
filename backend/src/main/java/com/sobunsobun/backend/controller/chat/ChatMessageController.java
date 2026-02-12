package com.sobunsobun.backend.controller.chat;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.domain.chat.ChatMessageType;
import com.sobunsobun.backend.dto.chat.MessageResponse;
import com.sobunsobun.backend.dto.chat.MessageSendRequest;
import com.sobunsobun.backend.dto.chat.ReadMarkRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP WebSocket 메시지 처리
 *
 * 라우팅:
 * - /app/chat.send    → /topic/rooms/{roomId} (메시지 전송)
 * - /app/chat.read    → /user/{userId}/queue/private (읽음 처리)
 * - /app/chat.invite  → 초대장 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송
     *
     * 클라: SEND /app/chat/send
     * 서버: /topic/rooms/{roomId} 브로드캐스트
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload MessageSendRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📤 [메시지 전송 시작] 요청 수신");

            Long userId = extractUserId(principal, headerAccessor);
            if (userId == null) {
                log.error("❌ [메시지 전송 실패] User ID를 찾을 수 없음");
                sendErrorToUser(null, "User authentication failed");
                return;
            }
            log.info("✅ [인증] userId 추출 성공: {}", userId);

            log.info("📝 [요청 정보] roomId: {}, contentLength: {}, type: {}",
                    request.getRoomId(),
                    request.getContent() != null ? request.getContent().length() : 0,
                    request.getType());

            // 메시지 저장
            log.debug("💾 [단계1] ChatMessageService.saveMessage() 호출 중...");
            MessageResponse response = chatMessageService.saveMessage(
                    request.getRoomId(),
                    userId,
                    request.getType(),
                    request.getContent(),
                    request.getImageUrl(),
                    request.getCardPayload()
            );
            log.info("✅ [단계1 완료] 메시지 저장됨: messageId={}", response.getId());

            // 채팅방의 모든 구독자에게 브로드캐스트
            String destination = "/topic/rooms/" + request.getRoomId();
            log.debug("📢 [단계2] 메시지 브로드캐스팅 중... destination: {}", destination);
            messagingTemplate.convertAndSend(destination, response);
            log.info("✅ [단계2 완료] 메시지 브로드캐스트 완료");

            log.info("✅ [메시지 전송 완료] roomId: {}, messageId: {}, receiver count: ?",
                    request.getRoomId(), response.getId());
            log.info("═════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [메시지 전송 오류] 예외 발생", e);
            log.error("   - roomId: {}", request != null ? request.getRoomId() : "unknown");
            log.error("   - content: {}", request != null && request.getContent() != null ? request.getContent().substring(0, Math.min(50, request.getContent().length())) : "null");
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            sendErrorToUser(extractUserId(principal, headerAccessor), e.getMessage());
        }
    }

    /**
     * 읽음 처리
     *
     * 클라: SEND /app/chat/read
     * 서버: 읽음 처리 후 채팅 목록 업데이트 알림
     */
    @MessageMapping("/chat/read")
    public void markAsRead(
            @Payload ReadMarkRequest request,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            log.info("═════════════════════════════════════════════════════════════");
            log.info("📖 [읽음 처리 시작] 요청 수신");

            Long userId = extractUserId(principal, headerAccessor);
            if (userId == null) {
                log.error("❌ [읽음 처리 실패] User ID를 찾을 수 없음");
                return;
            }
            log.info("✅ [인증] userId 추출 성공: {}", userId);

            log.info("📝 [요청 정보] roomId: {}, lastReadMessageId: {}",
                    request.getRoomId(), request.getLastReadMessageId());

            log.debug("💾 [단계1] ChatMessageService.markAsRead() 호출 중...");
            chatMessageService.markAsRead(
                    request.getRoomId(),
                    userId,
                    request.getLastReadMessageId()
            );
            log.info("✅ [단계1 완료] 읽음 처리 완료");

            // ✅ 읽음 처리 완료 - 개인 큐로 알림
            log.debug("📢 [단계2] 읽음 완료 알림 전송 중... userId: {}", userId);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/private",
                    java.util.Map.of(
                            "type", "READ_COMPLETE",
                            "roomId", request.getRoomId(),
                            "message", "✅ 읽음 처리 완료"
                    )
            );
            log.info("✅ [단계2 완료] 읽음 완료 알림 전송됨");

            log.info("✅ [읽음 처리 완료] roomId: {}, userId: {}",
                    request.getRoomId(), userId);
            log.info("═════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("═════════════════════════════════════════════════════════════");
            log.error("❌ [읽음 처리 오류] 예외 발생", e);
            log.error("   - roomId: {}", request != null ? request.getRoomId() : "unknown");
            log.error("   - errorMsg: {}", e.getMessage());
            log.error("═════════════════════════════════════════════════════════════");

            sendErrorToUser(extractUserId(principal, headerAccessor), e.getMessage());
        }
    }

    /**
     * 초대장 수락
     *
     * 클라: SEND /app/chat/invite/accept
     */
    @MessageMapping("/chat/invite/accept")
    public void acceptInvite(
            @Payload Map<String, Object> payload,
            Principal principal
    ) {
        try {
            Long userId = Long.parseLong(principal.getName());
            Long inviteId = ((Number) payload.get("inviteId")).longValue();
            Long targetRoomId = ((Number) payload.get("targetRoomId")).longValue();

            log.info("🎯 초대 수락 - inviteId: {}, userId: {}", inviteId, userId);

            // TODO: ChatInviteService 연동

        } catch (Exception e) {
            log.error("❌ 초대 수락 오류: {}", e.getMessage());
        }
    }

    // 유틸리티 메서드
    private Long extractUserId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        // 1. JwtUserPrincipal에서 추출
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof JwtUserPrincipal) {
                JwtUserPrincipal jwtPrincipal = (JwtUserPrincipal) auth.getPrincipal();
                return jwtPrincipal.id();
            }
        } catch (Exception e) {
            log.debug("⚠️ JwtUserPrincipal 캐스팅 실패");
        }

        // 2. Principal에서 직접 파싱
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (Exception e) {
                // ignore
            }
        }

        // 3. 세션 속성에서 찾기
        if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
            Object userId = headerAccessor.getSessionAttributes().get("userId");
            if (userId != null) {
                try {
                    return Long.parseLong(userId.toString());
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return null;
    }

    private void sendErrorToUser(Long userId, String message) {
        if (userId != null) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    "❌ Error: " + message
            );
        }
    }
}
