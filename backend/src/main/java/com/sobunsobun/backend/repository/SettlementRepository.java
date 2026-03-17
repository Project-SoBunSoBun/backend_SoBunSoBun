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
     * 내 정산 목록 (방장이거나 참여자인 정산, 상태 필터 없음)
     */
    @Query(value = "SELECT DISTINCT s FROM Settlement s " +
                   "JOIN FETCH s.groupPost p " +
                   "JOIN FETCH p.owner " +
                   "LEFT JOIN s.participants sp " +
                   "WHERE p.owner.id = :userId OR sp.user.id = :userId " +
                   "ORDER BY s.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT s) FROM Settlement s " +
                        "LEFT JOIN s.participants sp " +
                        "WHERE s.groupPost.owner.id = :userId OR sp.user.id = :userId")
    Page<Settlement> findByOwnerOrParticipant(@Param("userId") Long userId, Pageable pageable);

    /**
     * 내 정산 목록 (방장이거나 참여자인 정산, 상태 필터)
     */
    @Query(value = "SELECT DISTINCT s FROM Settlement s " +
                   "JOIN FETCH s.groupPost p " +
                   "JOIN FETCH p.owner " +
                   "LEFT JOIN s.participants sp " +
                   "WHERE (p.owner.id = :userId OR sp.user.id = :userId) AND s.status = :status " +
                   "ORDER BY s.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT s) FROM Settlement s " +
                        "LEFT JOIN s.participants sp " +
                        "WHERE (s.groupPost.owner.id = :userId OR sp.user.id = :userId) AND s.status = :status")
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
