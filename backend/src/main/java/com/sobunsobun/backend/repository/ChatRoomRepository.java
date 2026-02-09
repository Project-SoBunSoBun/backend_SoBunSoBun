package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.ChatRoom;
import com.sobunsobun.backend.domain.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ChatRoom 저장소
 *
 * 쿼리 최적화:
 * - 채팅방 목록은 LEFT JOIN (멤버가 없어도 조회)
 * - lastMessageAt 정렬 (최신순)
 * - N+1 쿼리 방지를 위해 fetch join 활용
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 특정 사용자의 채팅방 목록 (페이징)
     *
     * ACTIVE 멤버만 포함하고 최신순 정렬
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 채팅방 목록
     */
    @Query("""
            SELECT DISTINCT r FROM ChatRoom r
            LEFT JOIN FETCH r.members m
            WHERE m.user.id = :userId AND m.status = 'ACTIVE'
            ORDER BY r.lastMessageAt DESC NULLS LAST, r.updatedAt DESC
            """)
    Page<ChatRoom> findUserChatRooms(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 타입의 채팅방 목록 (개인 또는 단체)
     *
     * @param userId 사용자 ID
     * @param roomType 채팅방 타입
     * @param pageable 페이징 정보
     * @return 채팅방 목록
     */
    @Query("""
            SELECT DISTINCT r FROM ChatRoom r
            LEFT JOIN FETCH r.members m
            WHERE m.user.id = :userId AND m.status = 'ACTIVE' AND r.roomType = :roomType
            ORDER BY r.lastMessageAt DESC NULLS LAST, r.updatedAt DESC
            """)
    Page<ChatRoom> findUserChatRoomsByType(
            @Param("userId") Long userId,
            @Param("roomType") ChatRoomType roomType,
            Pageable pageable
    );

    /**
     * 1:1 개인 채팅방 조회
     *
     * 두 사용자 간의 개인 채팅방이 존재하는지 확인
     *
     * @param userId1 사용자 ID 1
     * @param userId2 사용자 ID 2
     * @return 채팅방 (있으면)
     */
    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.roomType = 'PRIVATE'
            AND (
                (SELECT COUNT(m) FROM ChatMember m 
                 WHERE m.chatRoom = r AND m.user.id = :userId1 AND m.status = 'ACTIVE') > 0
                AND
                (SELECT COUNT(m) FROM ChatMember m 
                 WHERE m.chatRoom = r AND m.user.id = :userId2 AND m.status = 'ACTIVE') > 0
            )
            """)
    Optional<ChatRoom> findPrivateChatRoom(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    /**
     * 단체 채팅방 조회 (GroupPost ID로)
     *
     * @param groupPostId 모임 ID
     * @return 채팅방 (있으면)
     */
    Optional<ChatRoom> findByGroupPostIdAndRoomType(Long groupPostId, ChatRoomType roomType);

    /**
     * 활성 멤버가 있는 채팅방 조회
     *
     * @param id 채팅방 ID
     * @return 채팅방
     */
    @Query("""
            SELECT r FROM ChatRoom r
            LEFT JOIN FETCH r.members m
            WHERE r.id = :id
            """)
    Optional<ChatRoom> findByIdWithMembers(@Param("id") Long id);

    /**
     * 특정 기간에 마지막 메시지가 있는 채팅방 조회
     *
     * 성능 최적화: 지난 메시지 정리용
     *
     * @param before 기준 시간
     * @return 채팅방 목록
     */
    List<ChatRoom> findByLastMessageAtBefore(LocalDateTime before);
}
