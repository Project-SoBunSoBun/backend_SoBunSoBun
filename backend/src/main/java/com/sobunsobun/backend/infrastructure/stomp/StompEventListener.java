package com.sobunsobun.backend.infrastructure.stomp;

import com.sobunsobun.backend.application.chat.ChatMessageService;
import com.sobunsobun.backend.infrastructure.redis.ChatRedisService;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP WebSocket 이벤트 리스너
 *
 * 사용자가 채팅방에 들어오고 나갈 때를 감지하여
 * Redis 상태 관리를 수행합니다.
 *
 * 감지하는 이벤트:
 * 1. SimpSubscribeEvent: 클라이언트가 /sub/chat/room/{roomId}를 구독할 때 (입장)
 * 2. SimpSessionDisconnectEvent: WebSocket 연결 끊김 또는 방 구독 취소할 때 (퇴장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final ChatRedisService chatRedisService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    // /topic/chat/room/{roomId} 형식의 destination에서 roomId를 추출하기 위한 정규식
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/topic/chat/room/(\\d+)");

    /**
     * 구독 ID → roomId 매핑 ("sessionId:subscriptionId" → roomId)
     *
     * STOMP UNSUBSCRIBE 프레임에는 destination이 포함되지 않으므로,
     * SUBSCRIBE 시점에 저장해두고 UNSUBSCRIBE 시 roomId를 역조회합니다.
     * onDisconnect 시 해당 세션의 항목을 일괄 제거합니다.
     */
    private final ConcurrentHashMap<String, Long> subscriptionRoomMap = new ConcurrentHashMap<>();

    /**
     * 사용자가 특정 채팅방을 구독할 때 호출
     *
     * 이벤트: SessionSubscribeEvent
     * 트리거: 클라이언트가 SUBSCRIBE 메시지를 보낼 때
     * 대상: /topic/chat/room/{roomId}
     *
     * 처리:
     * 1. destination에서 roomId 추출
     * 2. Principal에서 userId 추출
     * 3. ChatRedisService.enterRoom() 호출
     *
     * @param event STOMP Subscribe 이벤트
     */
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        try {
            log.info("📡 [STOMP Subscribe 이벤트 감지]");

            // 1. destination에서 roomId 추출
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
            String destination = headerAccessor.getDestination();
            Long roomId = extractRoomId(destination);

            if (roomId == null) {
                log.debug("⏭️ [스킵] 채팅방이 아닌 destination: {}", destination);
                return;
            }

            // 2. Principal에서 userId 추출
            Principal principal = event.getUser();
            Long userId = extractUserId(principal);

            if (userId == null) {
                log.error("❌ [오류] userId를 추출할 수 없음");
                return;
            }

            log.info("🚪 [채팅방 입장 이벤트] userId: {}, roomId: {}, destination: {}",
                    userId, roomId, destination);

            // 3. subscriptionId 기반 맵에 저장 (unsubscribe 시 roomId 역조회용)
            String sessionId = headerAccessor.getSessionId();
            String subscriptionId = headerAccessor.getSubscriptionId();
            if (sessionId != null && subscriptionId != null) {
                subscriptionRoomMap.put(sessionId + ":" + subscriptionId, roomId);
            }

            // 4. ChatRedisService.enterRoom() 호출 (Redis 리셋 + DB lastReadAt 갱신)
            chatRedisService.enterRoom(userId, roomId);

            // 5. 입장한 유저에게 완전한 CHAT_LIST_UPDATE 알림 전송 (unreadCount=0, roomName 등 포함)
            chatMessageService.sendEnterRoomNotification(userId, roomId);

            log.info("✅ [채팅방 입장 처리 완료] userId: {}, roomId: {}", userId, roomId);

        } catch (Exception e) {
            log.error("❌ [Subscribe 이벤트 처리 오류] error: {}", e.getMessage(), e);
        }
    }

    /**
     * 클라이언트가 채팅방 구독을 취소할 때 호출 (WebSocket 연결은 유지)
     *
     * 이벤트: SessionUnsubscribeEvent
     * 트리거: 클라이언트가 UNSUBSCRIBE 메시지를 보낼 때 (채팅방 화면에서 나갈 때)
     *
     * STOMP UNSUBSCRIBE 프레임에는 destination이 없으므로,
     * onSubscribe 시 저장해 둔 subscriptionRoomMap에서 roomId를 역조회합니다.
     *
     * active_room이 해당 방일 때만 leaveRoom()을 호출합니다.
     * (이미 다른 방에 입장한 경우 active_room이 overwrite되어 있으므로 건드리지 않음)
     */
    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        try {
            SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String subscriptionId = headerAccessor.getSubscriptionId();

            // subscriptionRoomMap에서 roomId 역조회
            Long roomId = null;
            if (sessionId != null && subscriptionId != null) {
                roomId = subscriptionRoomMap.remove(sessionId + ":" + subscriptionId);
            }

            if (roomId == null) {
                log.debug("⏭️ [Unsubscribe 스킵] 채팅방 구독이 아님 (sessionId: {})", sessionId);
                return;
            }

            Principal principal = event.getUser();
            Long userId = extractUserId(principal);

            if (userId == null) {
                log.debug("⏭️ [Unsubscribe 스킵] userId 추출 실패");
                return;
            }

            log.info("🚪 [채팅방 구독 취소] userId: {}, roomId: {}", userId, roomId);

            // active_room이 이 방일 때만 클리어 (다른 방으로 이동 후 이전 방 구독 취소 시 무시)
            Long currentActiveRoom = chatRedisService.getActiveRoom(userId);
            if (roomId.equals(currentActiveRoom)) {
                chatRedisService.leaveRoom(userId);
                log.info("✅ [active_room 초기화] userId: {}, roomId: {}", userId, roomId);
            } else {
                log.debug("⏭️ [active_room 유지] active_room({}) != unsubscribed roomId({})", currentActiveRoom, roomId);
            }

        } catch (Exception e) {
            log.error("❌ [Unsubscribe 이벤트 처리 오류] error: {}", e.getMessage(), e);
        }
    }

    /**
     * WebSocket 연결이 끊어질 때 호출
     *
     * 이벤트: SessionDisconnectEvent
     * 트리거: 클라이언트 연결 끊김, 타임아웃, 또는 명시적 disconnect
     *
     * 처리:
     * 1. subscriptionRoomMap에서 해당 세션 항목 일괄 제거
     * 2. Principal에서 userId 추출
     * 3. ChatRedisService.leaveRoom() 호출
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        try {
            log.info("📡 [STOMP Disconnect 이벤트 감지]");

            // 1. 해당 세션의 구독 맵 항목 일괄 제거
            String sessionId = event.getSessionId();
            subscriptionRoomMap.keySet().removeIf(key -> key.startsWith(sessionId + ":"));

            // 2. Principal에서 userId 추출
            Principal principal = event.getUser();
            Long userId = extractUserId(principal);

            if (userId == null) {
                log.debug("⏭️ [스킵] userId를 추출할 수 없음 (비인증 연결일 수 있음)");
                return;
            }

            log.info("🚪 [WebSocket 연결 끊김] userId: {}, sessionId: {}",
                    userId, sessionId);

            // 3. ChatRedisService.leaveRoom() 호출
            chatRedisService.leaveRoom(userId);

            log.info("✅ [채팅방 퇴장 처리 완료] userId: {}", userId);

        } catch (Exception e) {
            log.error("❌ [Disconnect 이벤트 처리 오류] error: {}", e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * destination 문자열에서 roomId 추출
     *
     * 형식: /sub/chat/room/{roomId}
     * 예:   /sub/chat/room/123 → 123
     *
     * @param destination STOMP destination 문자열
     * @return 추출된 roomId (추출 실패 시 null)
     */
    private Long extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }

        Matcher matcher = ROOM_ID_PATTERN.matcher(destination);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("⚠️ [roomId 추출 실패] 숫자 변환 오류: {}", matcher.group(1));
                return null;
            }
        }

        return null;
    }

    /**
     * Principal에서 userId 추출
     *
     * 실제 구현에서는 다음과 같은 방식 중 하나로 userId를 추출:
     * 1. JWT Token 파싱
     * 2. SecurityContextHolder에서 Authentication 조회
     * 3. SessionAttributes에서 직접 조회
     * 4. Principal.getName()을 userId로 사용 (UserDetails의 username이 userId인 경우)
     *
     * 현재 예시: Principal.getName()을 Long으로 변환 (userId로 가정)
     *
     * @param principal STOMP 세션의 Principal
     * @return 추출된 userId (추출 실패 시 null)
     */
    private Long extractUserId(Principal principal) {
        if (principal == null) {
            return null;
        }

        try {
            // WebSocketAuthInterceptor가 설정한 UsernamePasswordAuthenticationToken에서 JwtUserPrincipal 추출
            if (principal instanceof UsernamePasswordAuthenticationToken auth) {
                Object innerPrincipal = auth.getPrincipal();
                if (innerPrincipal instanceof JwtUserPrincipal jwtPrincipal) {
                    return jwtPrincipal.id();
                }
            }

            // Fallback: getName()을 Long으로 변환 시도
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("⚠️ [userId 추출 실패] Principal.getName()을 Long으로 변환 불가: {}",
                    principal.getName());
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [userId 추출 실패] principal type: {}, error: {}",
                    principal.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
