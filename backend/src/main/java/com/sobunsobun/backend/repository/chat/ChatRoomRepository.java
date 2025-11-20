package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByIdAndOwnerId(Long id, Long ownerId);
}
