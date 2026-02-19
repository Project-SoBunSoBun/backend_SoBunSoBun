package com.sobunsobun.backend.repository.chat;

import com.sobunsobun.backend.domain.chat.ChatMember;
import com.sobunsobun.backend.domain.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        SELECT COUNT(m) > 0 FROM ChatMember m
        WHERE m.chatRoom.id = :roomId
        AND m.user.id = :userId
        AND m.status = 'ACTIVE'
    """)
    boolean isActiveMember(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );

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

    /**
     * 특정 채팅방의 모든 ACTIVE 멤버 조회
     *
     * @param roomId 채팅방 ID
     * @return 활성 상태의 멤버 리스트
     */
    @Query("""
        SELECT m FROM ChatMember m
        WHERE m.chatRoom.id = :roomId
        AND m.status = 'ACTIVE'
    """)
    List<ChatMember> findActiveMembersByRoomId(@Param("roomId") Long roomId);

    /**
     * 특정 채팅방의 모든 ACTIVE 멤버의 userId 조회
     *
     * @param roomId 채팅방 ID
     * @return 멤버의 userId 리스트
     */
    @Query("""
        SELECT m.user.id FROM ChatMember m
        WHERE m.chatRoom.id = :roomId
        AND m.status = 'ACTIVE'
    """)
    List<Long> findActiveMemberIdsByRoomId(@Param("roomId") Long roomId);

    /**
     * ChatMember의 lastReadAt 업데이트
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadAt 업데이트할 시간
     */
    @Modifying
    @Query("""
        UPDATE ChatMember m
        SET m.lastReadAt = :lastReadAt
        WHERE m.chatRoom.id = :roomId
        AND m.user.id = :userId
    """)
    void updateLastReadAtByRoomIdAndUserId(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("lastReadAt") LocalDateTime lastReadAt
    );

    /**
     * 특정 사용자가 속한 모든 ACTIVE 채팅방 조회
     *
     * @param userId 사용자 ID
     * @return 사용자가 속한 ChatMember 리스트
     */
    @Query("""
        SELECT m FROM ChatMember m
        JOIN FETCH m.chatRoom
        WHERE m.user.id = :userId
        AND m.status = 'ACTIVE'
        ORDER BY m.chatRoom.lastMessageAt DESC
    """)
    List<ChatMember> findChatRoomsByUserId(@Param("userId") Long userId);

    /**
     * 두 사용자 간의 1:1 채팅방 조회
     *
     * user1과 user2가 모두 속한 PRIVATE 채팅방을 조회합니다.
     * (user1, user2) 순서와 (user2, user1) 순서 모두 동일한 방을 반환합니다.
     *
     * @param user1Id 첫 번째 사용자 ID
     * @param user2Id 두 번째 사용자 ID
     * @return 두 사용자 간의 1:1 채팅방 (있으면 Optional.of, 없으면 Optional.empty())
     */
    @Query("""
        SELECT DISTINCT m1.chatRoom
        FROM ChatMember m1
        JOIN ChatMember m2 ON m1.chatRoom.id = m2.chatRoom.id
        WHERE m1.user.id = :user1Id
        AND m2.user.id = :user2Id
        AND m1.status = 'ACTIVE'
        AND m2.status = 'ACTIVE'
        AND m1.chatRoom.roomType = 'ONE_TO_ONE'
    """)
    Optional<ChatRoom> findOneToOneChatRoom(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id
    );
}

