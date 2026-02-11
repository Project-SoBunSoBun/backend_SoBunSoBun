package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.id = :id
    """)
    Optional<ChatRoom> findByIdWithMembers(@Param("id") Long id);

    @Query("""
        SELECT r FROM ChatRoom r
        JOIN r.members m
        WHERE m.user.id = :userId
        ORDER BY r.lastMessageAt DESC
    """)
    Page<ChatRoom> findUserChatRooms(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.roomType = 'PRIVATE'
        AND r.id IN (
            SELECT m.chatRoom.id FROM ChatMember m
            WHERE m.user.id = :userId1 AND m.status = 'ACTIVE'
        )
        AND r.id IN (
            SELECT m.chatRoom.id FROM ChatMember m
            WHERE m.user.id = :userId2 AND m.status = 'ACTIVE'
        )
    """)
    Optional<ChatRoom> findPrivateChatRoom(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    Optional<ChatRoom> findByGroupPostIdAndRoomType(Long groupPostId, String roomType);
}
