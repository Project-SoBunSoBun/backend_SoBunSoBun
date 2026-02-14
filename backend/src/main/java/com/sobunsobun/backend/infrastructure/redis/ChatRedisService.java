package com.sobunsobun.backend.infrastructure.redis;

import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 이용한 채팅 상태 관리 서비스
 *
 * 관리 항목:
 * 1. 유저의 현재 접속 방 (user:{userId}:active_room)
 * 2. 방별 안 읽은 메시지 카운트 (room:{roomId}:user:{userId}:unread)
 *
 * 특징:
 * - 빠른 조회: DB 조회 없이 Redis에서 즉시 처리
 * - 자동 만료: TTL 설정으로 abandoned 데이터 자동 정리
 * - 트랜잭션 안전: 중요한 작업은 @Transactional로 보호
 * - Redis 선택적: Redis가 없어도 다른 기능은 정상 작동 (채팅만 제한)
 */
@Slf4j
@Service
public class ChatRedisService {

    private final Optional<RedisTemplate<String, String>> redisTemplate;
    private final ChatMemberRepository chatMemberRepository;
    private boolean redisAvailable = false;

    // Redis Key 프리픽스
    private static final String USER_ACTIVE_ROOM_PREFIX = "user:";
    private static final String USER_ACTIVE_ROOM_SUFFIX = ":active_room";
    private static final String ROOM_UNREAD_PREFIX = "room:";
    private static final String ROOM_UNREAD_SUFFIX = ":unread";

    // TTL 설정 (24시간)
    private static final long REDIS_EXPIRE_TIME = 24;
    private static final TimeUnit REDIS_EXPIRE_UNIT = TimeUnit.HOURS;

    @Autowired
    public ChatRedisService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            ChatMemberRepository chatMemberRepository
    ) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.chatMemberRepository = chatMemberRepository;

        // Redis 사용 가능 여부 확인
        this.redisAvailable = this.redisTemplate.isPresent();

        if (redisAvailable) {
            try {
                // 연결 테스트
                this.redisTemplate.get().opsForValue().get("connection-test");
                log.info("✅ ChatRedisService - Redis 연결 확인됨");
            } catch (Exception e) {
                log.warn("⚠️ ChatRedisService - Redis 연결 실패: {}", e.getMessage());
                log.warn("⚠️ 채팅 기능이 제한됩니다. 다른 API는 정상 작동합니다.");
                this.redisAvailable = false;
            }
        } else {
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
            log.warn("⚠️ Redis 서버가 연결되지 않았습니다!");
            log.warn("⚠️ 채팅 기능을 사용하려면 Redis 서버를 시작해주세요.");
            log.warn("⚠️ 다른 API는 정상적으로 작동합니다.");
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
        }
    }

    /**
     * Redis 사용 가능 여부 확인
     */
    private boolean isRedisAvailable() {
        if (!redisAvailable) {
            log.warn("⚠️ Redis가 사용 불가능합니다. 채팅 기능이 제한됩니다.");
            return false;
        }
        return true;
    }

    /**
     * 사용자가 채팅방에 입장할 때 호출
     *
     * 처리 내용:
     * 1. Redis에 user:{userId}:active_room = roomId 설정
     * 2. 해당 방의 unread 카운트를 0으로 초기화 (또는 삭제)
     * 3. DB의 ChatMember.lastReadAt을 현재 시간으로 업데이트
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     */
    @Transactional
    public void enterRoom(Long userId, Long roomId) {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis 미사용: enterRoom 작업 건너뜀");
            return;
        }

        try {
            log.info("🚪 [채팅방 입장] userId: {}, roomId: {}", userId, roomId);

            // 1. Redis에 사용자의 현재 접속 방 저장
            String activeRoomKey = buildActiveRoomKey(userId);
            redisTemplate.get().opsForValue().set(
                    activeRoomKey,
                    String.valueOf(roomId),
                    REDIS_EXPIRE_TIME,
                    REDIS_EXPIRE_UNIT
            );
            log.debug("✅ [Redis] {}={}", activeRoomKey, roomId);

            // 2. 해당 방의 unread 카운트 초기화
            String unreadKey = buildUnreadKey(roomId, userId);
            redisTemplate.get().delete(unreadKey);
            log.debug("✅ [Redis] unread 카운트 초기화: {}", unreadKey);

            // 3. DB의 ChatMember.lastReadAt 업데이트
            try {
                chatMemberRepository.updateLastReadAtByRoomIdAndUserId(roomId, userId, LocalDateTime.now());
                log.debug("✅ [DB] ChatMember.lastReadAt 업데이트 완료: roomId={}, userId={}", roomId, userId);
            } catch (Exception e) {
                log.warn("⚠️ [DB 경고] lastReadAt 업데이트 실패 (Redis는 성공): {}", e.getMessage());
                // DB 업데이트 실패는 Redis 작업을 무효화하지 않음
            }

            log.info("✅ [채팅방 입장 완료] userId: {}, roomId: {}", userId, roomId);

        } catch (Exception e) {
            log.error("❌ [채팅방 입장 오류] userId: {}, roomId: {}, error: {}",
                    userId, roomId, e.getMessage(), e);
            throw new RuntimeException("Redis enterRoom 실패", e);
        }
    }

    /**
     * 사용자가 채팅방에서 나갈 때 호출
     *
     * 처리 내용:
     * 1. Redis에서 user:{userId}:active_room 삭제
     *
     * @param userId 사용자 ID
     */
    public void leaveRoom(Long userId) {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis 미사용: leaveRoom 작업 건너뜀");
            return;
        }

        try {
            log.info("🚪 [채팅방 퇴장] userId: {}", userId);

            // Redis에서 사용자의 현재 접속 방 정보 삭제
            String activeRoomKey = buildActiveRoomKey(userId);
            boolean deleted = Boolean.TRUE.equals(redisTemplate.get().delete(activeRoomKey));

            if (deleted) {
                log.debug("✅ [Redis] {} 삭제 완료", activeRoomKey);
            } else {
                log.debug("⚠️ [Redis] 삭제할 데이터 없음: {}", activeRoomKey);
            }

            log.info("✅ [채팅방 퇴장 완료] userId: {}", userId);

        } catch (Exception e) {
            log.error("❌ [채팅방 퇴장 오류] userId: {}, error: {}",
                    userId, e.getMessage(), e);
            // 퇴장 시 오류는 로그만 남기고 던지지 않음 (graceful)
        }
    }

    /**
     * 메시지 발송 시 접속하지 않은 멤버의 안 읽은 카운트 증가
     *
     * 처리 내용:
     * 1. memberIds 중에서 현재 다른 방에 접속 중인 유저 필터링
     * 2. 해당 유저들의 unread 카운트를 1씩 증가
     * 3. senderId는 제외 (발신자 자신의 카운트는 증가하지 않음)
     *
     * 로직:
     * - user:{userId}:active_room이 현재 roomId와 다르거나 존재하지 않으면 미접속
     * - 미접속 유저의 room:{roomId}:user:{userId}:unread을 increment
     *
     * @param roomId 메시지가 발송된 방 ID
     * @param senderId 메시지 발신자 ID
     * @param memberIds 방의 모든 멤버 ID 리스트
     */
    public void addUnreadMessageCount(Long roomId, Long senderId, List<Long> memberIds) {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis 미사용: addUnreadMessageCount 작업 건너뜀");
            return;
        }

        try {
            log.info("🔔 [안 읽은 메시지 카운트 증가] roomId: {}, senderId: {}, memberCount: {}",
                    roomId, senderId, memberIds.size());

            for (Long memberId : memberIds) {
                // 발신자 자신은 제외
                if (memberId.equals(senderId)) {
                    log.debug("⏭️ [스킵] 발신자는 제외: userId={}", memberId);
                    continue;
                }

                // 해당 유저의 현재 접속 방 확인
                String activeRoomKey = buildActiveRoomKey(memberId);
                String activeRoom = redisTemplate.get().opsForValue().get(activeRoomKey);

                // 현재 방에 접속 중이 아닌 경우에만 unread 카운트 증가
                if (activeRoom == null || !activeRoom.equals(String.valueOf(roomId))) {
                    String unreadKey = buildUnreadKey(roomId, memberId);
                    Long newCount = redisTemplate.get().opsForValue().increment(unreadKey);

                    // TTL 설정 (이전에 설정되지 않았을 경우)
                    redisTemplate.get().expire(unreadKey, REDIS_EXPIRE_TIME, REDIS_EXPIRE_UNIT);

                    log.debug("✅ [증가] roomId: {}, userId: {}, newCount: {}",
                            roomId, memberId, newCount);
                } else {
                    log.debug("⏭️ [스킵] 현재 방에 접속 중: userId={}, roomId={}", memberId, roomId);
                }
            }

            log.info("✅ [안 읽은 메시지 카운트 증가 완료]");

        } catch (Exception e) {
            log.error("❌ [안 읽은 메시지 카운트 증가 오류] roomId: {}, error: {}",
                    roomId, e.getMessage(), e);
            // unread 카운트는 부가 기능이므로 실패해도 메시지 발송은 계속 진행
        }
    }

    /**
     * 특정 방의 특정 유저의 안 읽은 메시지 카운트 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 안 읽은 메시지 카운트 (없으면 0)
     */
    public Long getUnreadCount(Long roomId, Long userId) {
        if (!isRedisAvailable()) {
            return 0L;
        }

        try {
            String unreadKey = buildUnreadKey(roomId, userId);
            String count = redisTemplate.get().opsForValue().get(unreadKey);

            if (count == null) {
                return 0L;
            }

            return Long.parseLong(count);
        } catch (Exception e) {
            log.warn("⚠️ [안 읽은 메시지 카운트 조회 실패] roomId: {}, userId: {}",
                    roomId, userId);
            return 0L;
        }
    }

    /**
     * 특정 방의 특정 유저의 안 읽은 메시지 카운트 리셋
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    public void resetUnreadCount(Long roomId, Long userId) {
        if (!isRedisAvailable()) {
            return;
        }

        try {
            String unreadKey = buildUnreadKey(roomId, userId);
            redisTemplate.get().delete(unreadKey);
            log.debug("✅ [unread 카운트 리셋] roomId: {}, userId: {}", roomId, userId);
        } catch (Exception e) {
            log.warn("⚠️ [unread 카운트 리셋 실패] roomId: {}, userId: {}",
                    roomId, userId);
        }
    }

    /**
     * 사용자의 현재 접속 방 조회
     *
     * @param userId 사용자 ID
     * @return 현재 접속 중인 방 ID (접속 중이 아니면 null)
     */
    public Long getActiveRoom(Long userId) {
        if (!isRedisAvailable()) {
            return null;
        }

        try {
            String activeRoomKey = buildActiveRoomKey(userId);
            String activeRoom = redisTemplate.get().opsForValue().get(activeRoomKey);

            if (activeRoom == null) {
                return null;
            }

            return Long.parseLong(activeRoom);
        } catch (Exception e) {
            log.warn("⚠️ [현재 접속 방 조회 실패] userId: {}", userId);
            return null;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 사용자의 현재 접속 방 Redis Key 생성
     * 형식: user:{userId}:active_room
     */
    private String buildActiveRoomKey(Long userId) {
        return USER_ACTIVE_ROOM_PREFIX + userId + USER_ACTIVE_ROOM_SUFFIX;
    }

    /**
     * 안 읽은 메시지 카운트 Redis Key 생성
     * 형식: room:{roomId}:user:{userId}:unread
     */
    private String buildUnreadKey(Long roomId, Long userId) {
        return ROOM_UNREAD_PREFIX + roomId + ":user:" + userId + ROOM_UNREAD_SUFFIX;
    }
}
