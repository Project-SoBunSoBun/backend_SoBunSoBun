package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.SettleUp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettleUpRepository extends JpaRepository<SettleUp, Long> {

    /**
     * 특정 공동구매 게시글의 정산 목록 조회
     */
    List<SettleUp> findByGroupPostId(Long groupPostId);

    /**
     * 특정 공동구매 게시글의 특정 상태 정산 목록 조회
     */
    List<SettleUp> findByGroupPostIdAndStatus(Long groupPostId, Integer status);

    /**
     * 특정 사용자가 생성한 정산 목록 조회 (삭제된 정산 제외, 페이징)
     */
    @Query("SELECT s FROM SettleUp s " +
           "WHERE s.settledBy.id = :settledById AND s.status != 3 " +
           "ORDER BY s.createdAt DESC")
    Page<SettleUp> findBySettledById(@Param("settledById") Long settledById, Pageable pageable);

    /**
     * 특정 사용자가 생성한 특정 상태 정산 목록 조회 (페이징)
     */
    Page<SettleUp> findBySettledByIdAndStatus(Long settledById, Integer status, Pageable pageable);

    /**
     * ID로 정산 조회 (GroupPost, User 함께 조회)
     */
    @Query("SELECT s FROM SettleUp s " +
           "JOIN FETCH s.groupPost gp " +
           "JOIN FETCH s.settledBy u " +
           "WHERE s.id = :id")
    Optional<SettleUp> findByIdWithDetails(@Param("id") Long id);

    /**
     * 정산 상태별 조회 (페이징)
     */
    Page<SettleUp> findByStatus(Integer status, Pageable pageable);

    /**
     * 전체 정산 목록 조회 (미정산만, 페이징)
     */
    @Query("SELECT s FROM SettleUp s " +
           "JOIN FETCH s.groupPost gp " +
           "JOIN FETCH s.settledBy u " +
           "WHERE s.status = 1 " +
           "ORDER BY s.createdAt DESC")
    Page<SettleUp> findAllUnsettledWithDetails(Pageable pageable);

    /**
     * 전체 정산 목록 조회 (정산완료만, 페이징)
     */
    @Query("SELECT s FROM SettleUp s " +
           "JOIN FETCH s.groupPost gp " +
           "JOIN FETCH s.settledBy u " +
           "WHERE s.status = 2 " +
           "ORDER BY s.createdAt DESC")
    Page<SettleUp> findAllSettledWithDetails(Pageable pageable);

    /**
     * 전체 정산 목록 조회 (삭제된 정산 제외, 페이징)
     */
    @Query("SELECT s FROM SettleUp s " +
           "JOIN FETCH s.groupPost gp " +
           "JOIN FETCH s.settledBy u " +
           "WHERE s.status != 3 " +
           "ORDER BY s.createdAt DESC")
    Page<SettleUp> findAllWithDetails(Pageable pageable);

    /**
     * 사용자별 전체 정산 목록 조회 (삭제된 정산 제외, 페이징)
     */
    @Query("SELECT s FROM SettleUp s " +
           "WHERE s.settledBy.id = :settledById AND s.status != 3 " +
           "ORDER BY s.createdAt DESC")
    Page<SettleUp> findAllBySettledById(@Param("settledById") Long settledById, Pageable pageable);

    /**
     * 특정 공동구매 게시글과 생성자로 정산 존재 여부 확인
     */
    boolean existsByGroupPostIdAndSettledById(Long groupPostId, Long settledById);

    /**
     * 특정 공동구매 게시글에 삭제되지 않은 정산이 존재하는지 확인 (status != 3)
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SettleUp s " +
           "WHERE s.groupPost.id = :groupPostId AND s.status != 3")
    boolean existsActiveSettleUpByGroupPostId(@Param("groupPostId") Long groupPostId);
}

