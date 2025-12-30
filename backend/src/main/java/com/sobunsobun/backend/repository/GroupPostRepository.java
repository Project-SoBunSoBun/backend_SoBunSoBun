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
import java.util.Optional;

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
     * 게시글의 최대 인원 조회
     * Projection을 사용하여 maxMembers 필드만 반환
     *
     * @param id 게시글 ID
     * @param ownerUserId 게시글 작성자 ID
     * @return 최대 인원 (maxMembers)
     */
    @Query("SELECT p.maxMembers FROM GroupPost p WHERE p.id = :id AND p.owner.id = :ownerUserId")
    Optional<Integer> findMaxMembersByIdAndOwnerUserId(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);
}
