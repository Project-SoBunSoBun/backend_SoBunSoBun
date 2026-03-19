package com.sobunsobun.backend.infrastructure.redis;

import com.sobunsobun.backend.repository.chat.ChatMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis를 이용한 채팅 상태 관리 서비스
 *
 * 관리 항목:
 * 1. 유저의 현재 접속 방  : String  "user:{userId}:active_room"        → value: roomId
 * 2. 방별 안 읽은 수      : Hash    "unread:counts:{userId}"            → field: roomId, value: count
 *
 * Hash 구조 선택 이유:
 * - 유저 단위로 모든 방의 카운트를 하나의 키로 관리 → 조회/초기화 효율적
 * - HINCRBY 원자적 증가 지원
 *
 * Write-through 캐싱:
 * - getUnreadCount() 호출 시 Hash field가 없으면 DB에서 계산 후 캐싱
 * - 서버 재기동 후에도 유저가 채팅 목록을 열 때 자동 복구
 */
@Slf4j
@Service
public class ChatRedisService {

    private final Optional<RedisTemplate<String, String>> redisTemplate;
    private final ChatMemberRepository chatMemberRepository;
    private boolean redisAvailable = false;

    // ── Key 상수 ──────────────────────────────────────────────────────────────
    private static final String USER_ACTIVE_ROOM_PREFIX = "user:";
    private static final String USER_ACTIVE_ROOM_SUFFIX = ":active_room";

    /** Hash key: "unread:counts:{userId}"  /  field: "{roomId}"  /  value: count */
    private static final String UNREAD_HASH_PREFIX = "unread:counts:";

    // ── TTL ───────────────────────────────────────────────────────────────────
    private static final long REDIS_EXPIRE_TIME = 24;
    private static final TimeUnit REDIS_EXPIRE_UNIT = TimeUnit.HOURS;

    @Autowired
    public ChatRedisService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            ChatMemberRepository chatMemberRepository
    ) {
        this.redisTemplate = Optional.ofNullable(redisTemplate);
        this.chatMemberRepository = chatMemberRepository;
        this.redisAvailable = this.redisTemplate.isPresent();

        if (redisAvailable) {
            try {
                this.redisTemplate.get().opsForValue().get("connection-test");
                log.info(" ChatRedisService - Redis 연결 확인됨");
            } catch (Exception e) {
                log.warn(" ChatRedisService - Redis 연결 실패: {}", e.getMessage());
                this.redisAvailable = false;
            }
        } else {
            log.warn(" ═══════════════════════════════════════════════════════");
            log.warn(" Redis 서버가 연결되지 않았습니다!");
            log.warn(" 채팅 기능을 사용하려면 Redis 서버를 시작해주세요.");
            log.warn(" 다른 API는 정상적으로 작동합니다.");
            log.warn(" ═══════════════════════════════════════════════════════");
        }
    }

    private boolean isRedisAvailable() {
        if (!redisAvailable) {
            log.warn(" Redis가 사용 불가능합니다. 채팅 기능이 제한됩니다.");
            return false;
        }
        return true;
    }

    // ── 채팅방 입장 ───────────────────────────────────────────────────────────

    /**
     * 사용자가 채팅방에 입장할 때 호출
     *
     * 1. Redis에 user:{userId}:active_room = roomId 설정
     * 2. 해당 방의 unread 카운트를 0으로 초기화
     * 3. DB의 ChatMember.lastReadAt을 현재 시간으로 업데이트
     */
    @Transactional
    public void enterRoom(Long userId, Long roomId) {
        if (!isRedisAvailable()) {
            log.warn(" Redis 미사용: enterRoom 작업 건너뜀");
            return;
        }

        try {
            log.info(" [채팅방 입장] userId: {}, roomId: {}", userId, roomId);

            // 1. 현재 접속 방 저장
            String activeRoomKey = buildActiveRoomKey(userId);
            redisTemplate.get().opsForValue().set(
                    activeRoomKey,
                    String.valueOf(roomId),
                    REDIS_EXPIRE_TIME,
                    REDIS_EXPIRE_UNIT
            );

            // 2. 해당 방의 unread 카운트 0으로 초기화 (Hash field를 "0"으로 명시 설정)
            resetUnreadCount(roomId, userId);

            // 3. DB lastReadAt 업데이트
            try {
                chatMemberRepository.updateLastReadAtByRoomIdAndUserId(roomId, userId, LocalDateTime.now());
                log.debug(" [DB] ChatMember.lastReadAt 업데이트 완료: roomId={}, userId={}", roomId, userId);
            } catch (Exception e) {
                log.warn(" [DB 경고] lastReadAt 업데이트 실패: {}", e.getMessage());
            }

            log.info(" [채팅방 입장 완료] userId: {}, roomId: {}", userId, roomId);

        } catch (Exception e) {
            log.error(" [채팅방 입장 오류] userId: {}, roomId: {}, error: {}", userId, roomId, e.getMessage(), e);
            throw new RuntimeException("Redis enterRoom 실패", e);
        }
    }

    // ── 채팅방 퇴장 ───────────────────────────────────────────────────────────

    /**
     * 사용자가 채팅방에서 나갈 때 호출 (Redis active_room 키 삭제)
     */
    public void leaveRoom(Long userId) {
        if (!isRedisAvailable()) return;

        try {
            String activeRoomKey = buildActiveRoomKey(userId);
            redisTemplate.get().delete(activeRoomKey);
            log.info(" [채팅방 퇴장 완료] userId: {}", userId);
        } catch (Exception e) {
            log.error(" [채팅방 퇴장 오류] userId: {}, error: {}", userId, e.getMessage());
        }
    }

    // ── 안 읽은 메시지 카운트 ─────────────────────────────────────────────────

    /**
     * 메시지 발송 시 미접속 멤버의 unread count를 HINCRBY로 1씩 증가
     *
     * - sender 본인은 제외
     * - user:{userId}:active_room == roomId인 유저(현재 해당 방 구독 중)는 제외
     *
     * Redis Key: unread:counts:{userId}
     * Field    : {roomId}
     * Command  : HINCRBY 1
     */
    public void addUnreadMessageCount(Long roomId, Long senderId, List<Long> memberIds) {
        if (!isRedisAvailable()) {
            log.warn(" Redis 미사용: addUnreadMessageCount 작업 건너뜀");
            return;
        }

        try {
            log.info(" [unread count 증가] roomId: {}, senderId: {}, memberCount: {}",
                    roomId, senderId, memberIds.size());

            for (Long memberId : memberIds) {
                if (memberId.equals(senderId)) continue;

                // 현재 해당 방에 접속 중인 유저는 카운트 증가하지 않음
                String activeRoom = redisTemplate.get().opsForValue().get(buildActiveRoomKey(memberId));
                if (String.valueOf(roomId).equals(activeRoom)) {
                    log.debug("⏭ [스킵] 현재 방 접속 중: userId={}", memberId);
                    continue;
                }

                // HINCRBY: unread:counts:{userId} → field {roomId} += 1
                String hashKey = buildUnreadHashKey(memberId);
                String field   = String.valueOf(roomId);
                redisTemplate.get().opsForHash().increment(hashKey, field, 1);
                redisTemplate.get().expire(hashKey, REDIS_EXPIRE_TIME, REDIS_EXPIRE_UNIT);

                log.debug(" [HINCRBY] hashKey={}, field={}", hashKey, field);
            }

        } catch (Exception e) {
            log.error(" [unread count 증가 오류] roomId: {}, error: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 안 읽은 메시지 수 조회
     *
     * 1순위: Redis Hash에서 HGET
     * 2순위: Cache Miss → DB에서 계산 후 Hash에 캐싱 (Write-through)
     *
     * NOT_SUPPORTED: 외부 트랜잭션(예: getChatRoomList readOnly)에 참여하지 않고 독립 실행.
     * countUnreadMessages 쿼리 실패 시 외부 트랜잭션이 rollback-only 오염되는 것을 방지.
     *
     * @return 안 읽은 수 (Redis 미사용 환경에서는 DB 직접 조회)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long getUnreadCount(Long roomId, Long userId) {
        if (!isRedisAvailable()) {
            // Redis 없을 때는 DB 직접 조회
            try {
                return chatMemberRepository.countUnreadMessages(roomId, userId);
            } catch (Exception e) {
                return 0L;
            }
        }

        try {
            String hashKey = buildUnreadHashKey(userId);
            String field   = String.valueOf(roomId);

            Object value = redisTemplate.get().opsForHash().get(hashKey, field);

            if (value != null) {
                return Long.parseLong(value.toString());
            }

            // ── Cache Miss: DB에서 계산 후 Write-through 캐싱 ──────────────────
            log.debug(" [Cache Miss] DB에서 unread count 계산 후 캐싱: roomId={}, userId={}", roomId, userId);
            long dbCount = chatMemberRepository.countUnreadMessages(roomId, userId);

            redisTemplate.get().opsForHash().put(hashKey, field, String.valueOf(dbCount));
            redisTemplate.get().expire(hashKey, REDIS_EXPIRE_TIME, REDIS_EXPIRE_UNIT);

            log.debug(" [Write-through] hashKey={}, field={}, count={}", hashKey, field, dbCount);
            return dbCount;

        } catch (Exception e) {
            log.warn(" [unread count 조회 실패] roomId: {}, userId: {}, error: {}", roomId, userId, e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 안 읽은 메시지 수를 0으로 초기화 (방 입장 / 읽음 처리 시)
     *
     * Hash field를 삭제하지 않고 "0"으로 명시 설정:
     * → 다음 getUnreadCount 호출 시 DB 폴백 없이 즉시 0 반환
     */
    public void resetUnreadCount(Long roomId, Long userId) {
        if (!isRedisAvailable()) return;

        try {
            String hashKey = buildUnreadHashKey(userId);
            String field   = String.valueOf(roomId);
            redisTemplate.get().opsForHash().put(hashKey, field, "0");
            redisTemplate.get().expire(hashKey, REDIS_EXPIRE_TIME, REDIS_EXPIRE_UNIT);
            log.debug(" [unread 초기화] hashKey={}, field={}", hashKey, field);
        } catch (Exception e) {
            log.warn(" [unread count 초기화 실패] roomId: {}, userId: {}", roomId, userId);
        }
    }

    // ── 현재 접속 방 조회 ─────────────────────────────────────────────────────

    /**
     * 사용자의 현재 접속 방 ID 조회 (없으면 null)
     */
    public Long getActiveRoom(Long userId) {
        if (!isRedisAvailable()) return null;

        try {
            String activeRoom = redisTemplate.get().opsForValue().get(buildActiveRoomKey(userId));
            return activeRoom != null ? Long.parseLong(activeRoom) : null;
        } catch (Exception e) {
            log.warn(" [현재 접속 방 조회 실패] userId: {}", userId);
            return null;
        }
    }

    // ── Key 빌더 ──────────────────────────────────────────────────────────────

    /** "user:{userId}:active_room" */
    private String buildActiveRoomKey(Long userId) {
        return USER_ACTIVE_ROOM_PREFIX + userId + USER_ACTIVE_ROOM_SUFFIX;
    }

    /** "unread:counts:{userId}" */
    private String buildUnreadHashKey(Long userId) {
        return UNREAD_HASH_PREFIX + userId;
    }
}
