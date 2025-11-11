package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
     */
    Page<GroupPost> findByCategoriesAndStatusOrderByCreatedAtDesc(String categories, PostStatus status, Pageable pageable);

    /**
     * 마감일이 지난 OPEN 상태 게시글 조회
     */
    List<GroupPost> findByStatusAndDeadlineAtBefore(PostStatus status, LocalDateTime dateTime);
}

