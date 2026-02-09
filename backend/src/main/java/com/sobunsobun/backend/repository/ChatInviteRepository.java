package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.ChatInvite;
import com.sobunsobun.backend.domain.ChatInviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ChatInvite 저장소
 *
 * 역할:
 * - 초대장 조회 (수락/거절용)
 * - 초대 상태 관리
 */
@Repository
public interface ChatInviteRepository extends JpaRepository<ChatInvite, Long> {

    /**
     * 특정 사용자가 받은 초대장 목록
     *
     * @param inviteeId 초대받은 사용자 ID
     * @return 초대장 목록
     */
    @Query("""
            SELECT i FROM ChatInvite i
            WHERE i.invitee.id = :inviteeId AND i.status = 'PENDING'
            ORDER BY i.createdAt DESC
            """)
    List<ChatInvite> findPendingInvitesByInvitee(@Param("inviteeId") Long inviteeId);

    /**
     * 특정 채팅방에서 특정 사용자에게 보낸 초대장 조회
     *
     * 중복 초대 방지용
     *
     * @param chatRoomId 채팅방 ID
     * @param inviteeId 초대받은 사용자 ID
     * @return 초대장 (있으면)
     */
    @Query("""
            SELECT i FROM ChatInvite i
            WHERE i.chatRoom.id = :chatRoomId AND i.invitee.id = :inviteeId
            ORDER BY i.createdAt DESC
            LIMIT 1
            """)
    Optional<ChatInvite> findLatestInviteForRoom(
            @Param("chatRoomId") Long chatRoomId,
            @Param("inviteeId") Long inviteeId
    );

    /**
     * 만료된 초대장 조회
     *
     * 정기 정리 작업용
     *
     * @param before 기준 시간
     * @return 만료된 초대장 목록
     */
    @Query("""
            SELECT i FROM ChatInvite i
            WHERE i.status = 'PENDING' AND i.expiresAt < :before
            """)
    List<ChatInvite> findExpiredInvites(@Param("before") LocalDateTime before);

    /**
     * 특정 사용자의 받은 초대장 총 개수 (PENDING)
     *
     * @param inviteeId 초대받은 사용자 ID
     * @return 초대장 개수
     */
    @Query("""
            SELECT COUNT(i) FROM ChatInvite i
            WHERE i.invitee.id = :inviteeId AND i.status = 'PENDING'
            """)
    long countPendingInvites(@Param("inviteeId") Long inviteeId);
}
