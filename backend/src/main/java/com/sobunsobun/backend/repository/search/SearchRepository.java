package com.sobunsobun.backend.repository.search;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 검색 전용 리포지토리
 * 공동구매 게시글 검색 기능 담당
 */
@Repository
public interface SearchRepository extends JpaRepository<GroupPost, Long> {

    /**
     * 검색어로 게시글 검색 (OPEN 상태만)
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 게시글 목록 (페이징)
     */
    @Query("SELECT p FROM GroupPost p WHERE " +
           "(p.title LIKE %:keyword% OR " +
           "p.categories LIKE %:keyword% OR " +
           "p.itemsText LIKE %:keyword% OR " +
           "p.locationName LIKE %:keyword%) " +
           "AND p.status = com.sobunsobun.backend.domain.PostStatus.OPEN " +
           "ORDER BY p.createdAt DESC")
    Page<GroupPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 검색어로 게시글 검색 (마감임박순, OPEN 상태만)
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 게시글 목록 (페이징)
     */
    @Query("SELECT p FROM GroupPost p WHERE " +
           "(p.title LIKE %:keyword% OR " +
           "p.categories LIKE %:keyword% OR " +
           "p.itemsText LIKE %:keyword% OR " +
           "p.locationName LIKE %:keyword%) " +
           "AND p.status = com.sobunsobun.backend.domain.PostStatus.OPEN " +
           "ORDER BY p.deadlineAt ASC")
    Page<GroupPost> searchByKeywordOrderByDeadline(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 상태의 게시글만 검색
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param keyword 검색 키워드
     * @param status 게시글 상태
     * @param pageable 페이징 정보
     * @return 검색된 게시글 목록 (페이징)
     */
    @Query("SELECT p FROM GroupPost p WHERE " +
           "(p.title LIKE %:keyword% OR " +
           "p.categories LIKE %:keyword% OR " +
           "p.itemsText LIKE %:keyword% OR " +
           "p.locationName LIKE %:keyword%) " +
           "AND p.status = :status " +
           "ORDER BY p.createdAt DESC")
    Page<GroupPost> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                              @Param("status") PostStatus status,
                                              Pageable pageable);

    /**
     * 특정 상태의 게시글만 검색 (마감임박순)
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param keyword 검색 키워드
     * @param status 게시글 상태
     * @param pageable 페이징 정보
     * @return 검색된 게시글 목록 (페이징)
     */
    @Query("SELECT p FROM GroupPost p WHERE " +
           "(p.title LIKE %:keyword% OR " +
           "p.categories LIKE %:keyword% OR " +
           "p.itemsText LIKE %:keyword% OR " +
           "p.locationName LIKE %:keyword%) " +
           "AND p.status = :status " +
           "ORDER BY p.deadlineAt ASC")
    Page<GroupPost> searchByKeywordAndStatusOrderByDeadline(@Param("keyword") String keyword,
                                                             @Param("status") PostStatus status,
                                                             Pageable pageable);
}

