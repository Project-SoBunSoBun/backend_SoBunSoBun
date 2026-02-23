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
        SELECT DISTINCT r FROM ChatRoom r
        LEFT JOIN FETCH r.members m
        WHERE r.id = :id
    """)
    Optional<ChatRoom> findByIdWithMembers(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT r FROM ChatRoom r
        LEFT JOIN FETCH r.members m
        WHERE r.id IN (
            SELECT cm.chatRoom.id FROM ChatMember cm
            WHERE cm.user.id = :userId AND cm.status = 'ACTIVE'
        )
        ORDER BY r.lastMessageAt DESC NULLS LAST
    """)
    Page<ChatRoom> findUserChatRooms(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.roomType = 'ONE_TO_ONE'
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

    /**
     * 특정 공동구매 게시글에 연결된 단체 채팅방 조회
     *
     * @param groupPostId 공동구매 게시글 ID
     * @return 해당 게시글의 단체 채팅방 (있으면 Optional.of, 없으면 Optional.empty())
     */
    @Query("""
        SELECT r FROM ChatRoom r
        WHERE r.groupPost.id = :groupPostId
        AND r.roomType = 'GROUP'
    """)
    Optional<ChatRoom> findByGroupPostId(@Param("groupPostId") Long groupPostId);
}
