package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :roomId
        ORDER BY m.createdAt DESC
    """)
    Page<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :roomId
        AND m.createdAt < :cursor
        ORDER BY m.createdAt DESC
    """)
    Page<ChatMessage> findMessagesBeforeCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    /**
     * 특정 채팅방의 가장 최근 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @return 가장 최근 메시지 (없으면 Optional.empty())
     */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :roomId
        ORDER BY m.createdAt DESC
        LIMIT 1
    """)
    Optional<ChatMessage> findLatestMessageByRoomId(@Param("roomId") Long roomId);

    /**
     * 커서 기반 페이징: 특정 메시지 ID보다 작은(과거의) 메시지들을 조회
     *
     * 무한 스크롤(Infinite Scroll) 구현을 위한 메서드
     * lastMessageId가 null이면 가장 최신 메시지부터 시작
     *
     * @param roomId 채팅방 ID
     * @param lastMessageId 마지막으로 조회한 메시지 ID (커서)
     * @param size 조회할 메시지 개수
     * @return 과거 메시지 리스트 (내림차순, 가장 최신순)
     */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :roomId
        AND (:lastMessageId IS NULL OR m.id < :lastMessageId)
        ORDER BY m.id DESC
    """)
    List<ChatMessage> findMessagesByRoomIdAndMessageIdLessThanOrderByIdDesc(
            @Param("roomId") Long roomId,
            @Param("lastMessageId") Long lastMessageId,
            Pageable pageable
    );

    /**
     * 특정 사용자가 보낸 모든 메시지 삭제 (회원탈퇴용)
     */
    void deleteBySenderId(Long senderId);
}
