package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.ChatMessage;
import com.sobunsobun.backend.domain.ChatMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatMessage 저장소
 *
 * 설계:
 * - 커서 기반 페이징 (타임스탬프 기반)
 * - 메시지 타입별 조회 (TEXT, IMAGE, CARD 등)
 * - 성능 최적화: 인덱스 활용 (chat_room_id, created_at)
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 목록 (페이징)
     *
     * 최신순 정렬 (createdAt DESC)
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 메시지 페이지
     */
    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(
            Long chatRoomId,
            Pageable pageable
    );

    /**
     * 커서 기반 페이징: 특정 타임스탬프 이전 메시지 조회
     *
     * 사용 사례: 사용자가 위로 스크롤하여 이전 메시지 로드
     *
     * @param chatRoomId 채팅방 ID
     * @param createdAt 기준 타임스탐프
     * @param pageable 페이징 정보
     * @return 메시지 페이지
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId AND m.createdAt < :createdAt
            ORDER BY m.createdAt DESC
            """)
    Page<ChatMessage> findMessagesBeforeCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("createdAt") LocalDateTime createdAt,
            Pageable pageable
    );

    /**
     * 커서 기반 페이징: 특정 타임스탬프 이후 메시지 조회
     *
     * 사용 사례: 아래로 스크롤하여 새로운 메시지 로드 (거의 사용 안 함)
     *
     * @param chatRoomId 채팅방 ID
     * @param createdAt 기준 타임스탐프
     * @param pageable 페이징 정보
     * @return 메시지 페이지
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId AND m.createdAt > :createdAt
            ORDER BY m.createdAt ASC
            """)
    Page<ChatMessage> findMessagesAfterCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("createdAt") LocalDateTime createdAt,
            Pageable pageable
    );

    /**
     * 특정 타입의 메시지만 조회
     *
     * 예: INVITE_CARD, SETTLEMENT_CARD 등
     *
     * @param chatRoomId 채팅방 ID
     * @param type 메시지 타입
     * @return 메시지 목록
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId AND m.type = :type
            ORDER BY m.createdAt DESC
            """)
    List<ChatMessage> findByRoomAndType(
            @Param("chatRoomId") Long chatRoomId,
            @Param("type") ChatMessageType type
    );

    /**
     * 특정 시간 이후의 메시지 조회 (새 메시지 감지용)
     *
     * 웹소켓 재연결 시 놓친 메시지 조회
     *
     * @param chatRoomId 채팅방 ID
     * @param after 기준 시간
     * @return 메시지 목록
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId AND m.createdAt > :after
            ORDER BY m.createdAt ASC
            """)
    List<ChatMessage> findNewMessagesSince(
            @Param("chatRoomId") Long chatRoomId,
            @Param("after") LocalDateTime after
    );

    /**
     * 채팅방의 가장 최신 메시지 조회
     *
     * 목록에서 미리보기 표시용
     *
     * @param chatRoomId 채팅방 ID
     * @return 최신 메시지
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId
            ORDER BY m.createdAt DESC
            LIMIT 1
            """)
    ChatMessage findLatestMessage(@Param("chatRoomId") Long chatRoomId);

    /**
     * 채팅방의 메시지 총 개수
     *
     * @param chatRoomId 채팅방 ID
     * @return 메시지 개수
     */
    long countByChatRoomId(Long chatRoomId);

    /**
     * 특정 메시지보다 최신인 메시지 개수
     *
     * 미읽은 개수 계산용
     *
     * @param chatRoomId 채팅방 ID
     * @param messageId 기준 메시지 ID
     * @return 메시지 개수
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.chatRoom.id = :chatRoomId AND m.id > :messageId
            """)
    long countMessagesAfterId(
            @Param("chatRoomId") Long chatRoomId,
            @Param("messageId") Long messageId
    );
}
