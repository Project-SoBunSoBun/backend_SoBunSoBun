package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

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
}
