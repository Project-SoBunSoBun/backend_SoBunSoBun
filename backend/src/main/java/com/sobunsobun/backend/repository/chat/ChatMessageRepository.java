package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    // 최신 메시지 조회
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
}
