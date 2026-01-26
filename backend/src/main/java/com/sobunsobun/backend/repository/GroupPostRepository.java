package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공동구매 게시글 리포지토리
 */
@Repository
public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {

    /**
     * 상태별 게시글 조회 (마감일 오름차순)
     */
    Page<GroupPost> findByStatusOrderByDeadlineAtAsc(PostStatus status, Pageable pageable);

    /**
     * 작성자별 게시글 조회
     */
    Page<GroupPost> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    /**
     * 카테고리별 게시글 조회 (모집 중인 것만)
     * categories 컬럼에 해당 카테고리 코드가 포함된 게시글 검색 (LIKE 검색)
     */
    @Query("SELECT p FROM GroupPost p WHERE p.categories LIKE CONCAT('%', :categories, '%') AND p.status = :status ORDER BY p.createdAt DESC")
    Page<GroupPost> findByCategoriesAndStatusOrderByCreatedAtDesc(@Param("categories") String categories,
                                                                   @Param("status") PostStatus status,
                                                                   Pageable pageable);

    /**
     * 여러 카테고리로 게시글 조회 (모집 중인 것만)
     * categories 컬럼에 요청한 카테고리 중 하나라도 포함된 게시글 검색
     *
     * Native Query 사용: REGEXP를 활용하여 동적으로 여러 카테고리 검색
     */
    @Query(value = "SELECT * FROM group_post WHERE status = :status " +
                   "AND categories REGEXP :categoryPattern " +
                   "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM group_post WHERE status = :status " +
                        "AND categories REGEXP :categoryPattern",
           nativeQuery = true)
    Page<GroupPost> findByCategoriesInAndStatus(@Param("categoryPattern") String categoryPattern,
                                                  @Param("status") String status,
                                                  Pageable pageable);

    /**
     * 마감일이 지난 OPEN 상태 게시글 조회
     */
    List<GroupPost> findByStatusAndDeadlineAtBefore(PostStatus status, LocalDateTime dateTime);

    /**
     * 특정 날짜 이후의 OPEN 상태 게시글 조회 (추천 검색어 수집용)
     * 생성일 기준 최신순 정렬
     */
    @Query("SELECT p FROM GroupPost p WHERE p.status = 'OPEN' AND p.createdAt >= :startDate ORDER BY p.createdAt DESC")
    List<GroupPost> findRecentOpenPosts(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    /**
     * 사용자가 작성한 게시글 수 조회 (호스트 수)
     */
    long countByOwnerId(Long userId);

    /**
     * 사용자가 참여한 게시글 수 조회
     * TODO: 참여 정보를 저장하는 엔티티/테이블 필요
     */
    // long countByParticipantsId(Long userId);
}
