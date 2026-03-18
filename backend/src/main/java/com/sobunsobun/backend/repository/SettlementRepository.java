package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Settlement;
import com.sobunsobun.backend.domain.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * 게시글 ID로 정산 단건 조회
     */
    Optional<Settlement> findByGroupPostId(Long groupPostId);

    /**
     * 정산 ID로 상세 조회 (participants + items batch 로딩)
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.groupPost p " +
           "JOIN FETCH p.owner " +
           "LEFT JOIN FETCH s.participants sp " +
           "LEFT JOIN FETCH sp.user " +
           "WHERE s.id = :id")
    Optional<Settlement> findWithDetailById(@Param("id") Long id);

    /**
     * 게시글 ID로 상세 조회
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.groupPost p " +
           "LEFT JOIN FETCH s.participants sp " +
           "LEFT JOIN FETCH sp.user " +
           "WHERE s.groupPost.id = :groupPostId")
    Optional<Settlement> findWithDetailByGroupPostId(@Param("groupPostId") Long groupPostId);

    /**
     * 내 정산 목록 (방장이거나 참여자이거나 채팅방 활성 멤버인 정산, 상태 필터 없음)
     * - COMPLETED: SettlementParticipant 레코드로 판단
     * - PENDING: ChatMember ACTIVE 상태로 판단 (아직 참여자 레코드 없음)
     */
    @Query(value = "SELECT s FROM Settlement s " +
                   "JOIN FETCH s.groupPost p " +
                   "JOIN FETCH p.owner " +
                   "WHERE p.owner.id = :userId " +
                   "   OR EXISTS (SELECT 1 FROM SettlementParticipant sp " +
                   "              WHERE sp.settlement = s AND sp.user.id = :userId) " +
                   "   OR EXISTS (SELECT 1 FROM ChatMember cm " +
                   "              WHERE cm.chatRoom.groupPost = p " +
                   "              AND cm.user.id = :userId AND cm.status = 'ACTIVE') " +
                   "ORDER BY s.createdAt DESC",
           countQuery = "SELECT COUNT(s) FROM Settlement s " +
                        "WHERE s.groupPost.owner.id = :userId " +
                        "   OR EXISTS (SELECT 1 FROM SettlementParticipant sp " +
                        "              WHERE sp.settlement = s AND sp.user.id = :userId) " +
                        "   OR EXISTS (SELECT 1 FROM ChatMember cm " +
                        "              WHERE cm.chatRoom.groupPost = s.groupPost " +
                        "              AND cm.user.id = :userId AND cm.status = 'ACTIVE')")
    Page<Settlement> findByOwnerOrParticipant(@Param("userId") Long userId, Pageable pageable);

    /**
     * 내 정산 목록 (방장이거나 참여자이거나 채팅방 활성 멤버인 정산, 상태 필터)
     */
    @Query(value = "SELECT s FROM Settlement s " +
                   "JOIN FETCH s.groupPost p " +
                   "JOIN FETCH p.owner " +
                   "WHERE s.status = :status " +
                   "  AND (p.owner.id = :userId " +
                   "       OR EXISTS (SELECT 1 FROM SettlementParticipant sp " +
                   "                  WHERE sp.settlement = s AND sp.user.id = :userId) " +
                   "       OR EXISTS (SELECT 1 FROM ChatMember cm " +
                   "                  WHERE cm.chatRoom.groupPost = p " +
                   "                  AND cm.user.id = :userId AND cm.status = 'ACTIVE')) " +
                   "ORDER BY s.createdAt DESC",
           countQuery = "SELECT COUNT(s) FROM Settlement s " +
                        "WHERE s.status = :status " +
                        "  AND (s.groupPost.owner.id = :userId " +
                        "       OR EXISTS (SELECT 1 FROM SettlementParticipant sp " +
                        "                  WHERE sp.settlement = s AND sp.user.id = :userId) " +
                        "       OR EXISTS (SELECT 1 FROM ChatMember cm " +
                        "                  WHERE cm.chatRoom.groupPost = s.groupPost " +
                        "                  AND cm.user.id = :userId AND cm.status = 'ACTIVE'))")
    Page<Settlement> findByOwnerOrParticipantAndStatus(@Param("userId") Long userId,
                                                       @Param("status") SettlementStatus status,
                                                       Pageable pageable);

    /**
     * 정산 재완료 시 기존 품목 전체 삭제 (items FK 제약 때문에 participants 삭제 전에 먼저 삭제)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM SettlementItem si WHERE si.participant.settlement.id = :settlementId")
    void deleteItemsBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 정산 재완료 시 기존 참여자 전체 삭제
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM SettlementParticipant sp WHERE sp.settlement.id = :settlementId")
    void deleteParticipantsBySettlementId(@Param("settlementId") Long settlementId);

    /**
     * 게시글의 정산 완료 여부 (ChatRoomService 사용)
     */
    boolean existsByGroupPostIdAndStatus(Long groupPostId, SettlementStatus status);
}
