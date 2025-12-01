package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // 첫 로딩: roomId 기준 최신 N개
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    // 이전 로딩: cursor(createdAt) 보다 "이전" 메시지 N개
    Page<ChatMessage> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long roomId,
            Instant cursorCreatedAt,
            Pageable pageable
    );
}
