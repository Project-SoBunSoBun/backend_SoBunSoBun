package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Settlement;
import com.sobunsobun.backend.domain.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * 내 정산 목록 (내가 작성한 게시글의 정산, 상태 필터 없음)
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.groupPost p " +
           "WHERE p.owner.id = :userId " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByGroupPostOwnerId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 내 정산 목록 (상태 필터)
     */
    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.groupPost p " +
           "WHERE p.owner.id = :userId AND s.status = :status " +
           "ORDER BY s.createdAt DESC")
    Page<Settlement> findByGroupPostOwnerIdAndStatus(@Param("userId") Long userId,
                                                     @Param("status") SettlementStatus status,
                                                     Pageable pageable);

    /**
     * 게시글의 정산 완료 여부 (ChatRoomService 사용)
     */
    boolean existsByGroupPostIdAndStatus(Long groupPostId, SettlementStatus status);
}
