package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    @Query("""
        SELECT m FROM ChatMember m
        WHERE m.chatRoom.id = :roomId
        AND m.user.id = :userId
    """)
    Optional<ChatMember> findMember(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT m FROM ChatMember m
        WHERE m.chatRoom.id = :roomId
        AND m.status = 'ACTIVE'
    """)
    List<ChatMember> findActiveMembers(@Param("roomId") Long roomId);

    @Query("""
        SELECT COUNT(msg) FROM ChatMessage msg
        WHERE msg.chatRoom.id = :roomId
        AND msg.sender.id != :userId
        AND msg.id > COALESCE(
            (SELECT m.lastReadMessageId FROM ChatMember m 
             WHERE m.chatRoom.id = :roomId AND m.user.id = :userId), 0)
    """)
    long countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );
}

