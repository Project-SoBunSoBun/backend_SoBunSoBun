package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.ChatMember;
import com.sobunsobun.backend.domain.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ChatMember 저장소
 *
 * 역할:
 * - 채팅방 멤버 조회 (권한 검증용)
 * - 읽음 처리 정보 조회 (unreadCount 계산용)
 */
@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    /**
     * 특정 사용자의 특정 채팅방 멤버 정보 조회
     *
     * 권한 검증에 사용: 해당 room의 멤버인지 확인
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 멤버 정보 (없으면 empty)
     */
    @Query("""
            SELECT m FROM ChatMember m
            WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId
            """)
    Optional<ChatMember> findMember(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    /**
     * 특정 채팅방의 활성 멤버 목록
     *
     * @param chatRoomId 채팅방 ID
     * @return 활성 멤버 목록
     */
    @Query("""
            SELECT m FROM ChatMember m
            WHERE m.chatRoom.id = :chatRoomId AND m.status = 'ACTIVE'
            ORDER BY m.joinedAt ASC
            """)
    List<ChatMember> findActiveMembers(@Param("chatRoomId") Long chatRoomId);

    /**
     * 특정 채팅방의 모든 멤버 (퇴장 포함)
     *
     * @param chatRoomId 채팅방 ID
     * @return 모든 멤버 목록
     */
    List<ChatMember> findByChatRoomId(Long chatRoomId);

    /**
     * 사용자가 활성 멤버로 속한 채팅방 목록
     *
     * @param userId 사용자 ID
     * @return 채팅방 ID 목록
     */
    @Query("""
            SELECT m.chatRoom.id FROM ChatMember m
            WHERE m.user.id = :userId AND m.status = 'ACTIVE'
            """)
    List<Long> findActiveChatRoomIdsByUserId(@Param("userId") Long userId);

    /**
     * 채팅방 멤버 수 조회
     *
     * @param chatRoomId 채팅방 ID
     * @return 활성 멤버 수
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMember m
            WHERE m.chatRoom.id = :chatRoomId AND m.status = 'ACTIVE'
            """)
    long countActiveMembers(@Param("chatRoomId") Long chatRoomId);

    /**
     * 특정 사용자의 미읽은 메시지 개수 (특정 채팅방)
     *
     * 마지막 읽은 메시지 ID 기준으로 그 이후의 메시지 개수 계산
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 미읽은 메시지 개수
     */
    @Query("""
            SELECT COUNT(msg) FROM ChatMessage msg
            WHERE msg.chatRoom.id = :chatRoomId
            AND (
                (SELECT m.lastReadMessageId FROM ChatMember m
                 WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId) IS NULL
                OR msg.id > (SELECT m.lastReadMessageId FROM ChatMember m
                            WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId)
            )
            """)
    long countUnreadMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    /**
     * 특정 사용자의 특정 메시지 이후 미읽은 개수
     *
     * 커서 기반 페이징에서 사용
     *
     * @param chatRoomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastReadMessageId 마지막 읽은 메시지 ID
     * @return 미읽은 메시지 개수
     */
    @Query("""
            SELECT COUNT(msg) FROM ChatMessage msg
            WHERE msg.chatRoom.id = :chatRoomId AND msg.id > :lastReadMessageId
            """)
    long countUnreadMessagesAfter(
            @Param("chatRoomId") Long chatRoomId,
            @Param("lastReadMessageId") Long lastReadMessageId
    );
}
