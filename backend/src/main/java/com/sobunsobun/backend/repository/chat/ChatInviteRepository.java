package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatInvite;
import com.sobunsobun.backend.domain.chat.ChatInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatInviteRepository extends JpaRepository<ChatInvite, Long> {

    @Query("""
        SELECT i FROM ChatInvite i
        WHERE i.chatRoom.id = :roomId
        AND i.invitee.id = :inviteeId
        ORDER BY i.createdAt DESC
        LIMIT 1
    """)
    Optional<ChatInvite> findLatestInviteForRoom(
            @Param("roomId") Long roomId,
            @Param("inviteeId") Long inviteeId
    );

    @Query("""
        SELECT i FROM ChatInvite i
        WHERE i.invitee.id = :inviteeId
        AND i.status = 'PENDING'
        AND i.expiresAt > CURRENT_TIMESTAMP
        ORDER BY i.createdAt DESC
    """)
    List<ChatInvite> findPendingInvites(@Param("inviteeId") Long inviteeId);
}
