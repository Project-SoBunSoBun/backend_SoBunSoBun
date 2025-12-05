package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatRoom;
import com.sobunsobun.backend.enumClass.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByIdAndOwnerId(Long id, Long ownerId);

    // 만료된 채팅방 조회 (status가 CLOSED이고 expireAt이 현재 시간보다 이전인 채팅방)
    List<ChatRoom> findByStatusAndExpireAtBefore(ChatRoomStatus status, LocalDateTime expireAt);
}
