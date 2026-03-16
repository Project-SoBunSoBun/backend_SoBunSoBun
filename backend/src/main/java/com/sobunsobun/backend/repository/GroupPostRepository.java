package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.BlockedUser;
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
     * 작성자와 상태로 게시글 존재 여부 확인
     * 새 게시글 작성 전 진행 중인 게시글이 있는지 검증하는 데 사용
     *
     * @param ownerId 작성자 ID
     * @param statuses 확인할 상태 목록 (예: OPEN, IN_PROGRESS 등)
     * @return 해당 상태의 게시글이 존재하면 true
     */
    boolean existsByOwnerIdAndStatusIn(Long ownerId, List<PostStatus> statuses);

    /**
     * 상태별 게시글 조회 (마감일 오름차순)
     */
    Page<GroupPost> findByStatusOrderByDeadlineAtAsc(PostStatus status, Pageable pageable);

    /**
     * 작성자별 게시글 조회 (Pageable)
     */
    Page<GroupPost> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    /**
     * 작성자별 게시글 조회 - 특정 상태 제외 (Pageable)
     */
    Page<GroupPost> findByOwnerIdAndStatusNotOrderByCreatedAtDesc(Long ownerId, PostStatus status, Pageable pageable);

    /**
     * 작성자별 게시글 조회 (List - 프로필 조회용)
     * 사용자가 작성한 모든 게시글을 최신순으로 반환
     */
    List<GroupPost> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

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

    // ── 차단 유저 제외 쿼리 ─────────────────────────────────────────────────

    /**
     * 전체 게시글 조회 (차단 유저 제외, CANCELLED 제외, 최신순)
     */
    @Query("SELECT p FROM GroupPost p WHERE p.status != 'CANCELLED' AND p.owner.id NOT IN " +
           "(SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :viewerId) " +
           "ORDER BY p.createdAt DESC")
    Page<GroupPost> findAllExcludingBlocked(@Param("viewerId") Long viewerId, Pageable pageable);

    /**
     * 전체 게시글 조회 (비로그인, CANCELLED 제외, 최신순)
     */
    Page<GroupPost> findAllByStatusNotOrderByCreatedAtDesc(PostStatus status, Pageable pageable);

    /**
     * 상태별 게시글 조회 (차단 유저 제외, 마감일 오름차순)
     */
    @Query("SELECT p FROM GroupPost p WHERE p.status = :status AND p.owner.id NOT IN " +
           "(SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :viewerId) " +
           "ORDER BY p.deadlineAt ASC")
    Page<GroupPost> findByStatusExcludingBlocked(@Param("viewerId") Long viewerId,
                                                 @Param("status") PostStatus status,
                                                 Pageable pageable);

    /**
     * 단일 카테고리 게시글 조회 (차단 유저 제외, 최신순)
     */
    @Query("SELECT p FROM GroupPost p WHERE p.categories LIKE CONCAT('%', :categories, '%') " +
           "AND p.status = :status AND p.owner.id NOT IN " +
           "(SELECT b.blocked.id FROM BlockedUser b WHERE b.blocker.id = :viewerId) " +
           "ORDER BY p.createdAt DESC")
    Page<GroupPost> findByCategoriesAndStatusExcludingBlocked(@Param("viewerId") Long viewerId,
                                                              @Param("categories") String categories,
                                                              @Param("status") PostStatus status,
                                                              Pageable pageable);

    /**
     * 여러 카테고리 게시글 조회 (차단 유저 제외, Native Query)
     */
    @Query(value = "SELECT * FROM group_post WHERE status = :status " +
                   "AND categories REGEXP :categoryPattern " +
                   "AND owner_id NOT IN (SELECT blocked_id FROM blocked_users WHERE blocker_id = :viewerId) " +
                   "ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM group_post WHERE status = :status " +
                        "AND categories REGEXP :categoryPattern " +
                        "AND owner_id NOT IN (SELECT blocked_id FROM blocked_users WHERE blocker_id = :viewerId)",
           nativeQuery = true)
    Page<GroupPost> findByCategoriesInAndStatusExcludingBlocked(@Param("viewerId") Long viewerId,
                                                                @Param("categoryPattern") String categoryPattern,
                                                                @Param("status") String status,
                                                                Pageable pageable);

    // ────────────────────────────────────────────────────────────────────────

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

    /**
     * 사용자가 댓글을 단 게시글 조회 (페이징)
     * 중복 없이 게시글 단위로 반환 (한 게시글에 여러 댓글을 달아도 1번만 포함)
     */
    @Query(value = "SELECT p FROM GroupPost p WHERE p.id IN " +
                   "(SELECT c.post.id FROM Comment c WHERE c.user.id = :userId AND c.deleted = false)",
           countQuery = "SELECT COUNT(DISTINCT p.id) FROM GroupPost p WHERE p.id IN " +
                        "(SELECT c.post.id FROM Comment c WHERE c.user.id = :userId AND c.deleted = false)")
    Page<GroupPost> findPostsCommentedByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 사용자의 모든 게시글 삭제 (회원탈퇴용)
     */
    void deleteByOwnerId(Long ownerId);
}
