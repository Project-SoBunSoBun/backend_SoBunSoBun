package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 저장소
 * JPA를 사용한 Comment 엔티티 데이터 접근 계층
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글의 모든 활성 부모 댓글 조회 (대댓글 제외)
     * 삭제되지 않은 부모 댓글만 반환
     * 오래된순으로 정렬
     *
     * @param postId 게시글 ID
     * @return 부모 댓글 목록 (대댓글 제외)
     */
    @Query("SELECT c FROM Comment c " +
           "WHERE c.post.id = :postId " +
           "AND c.parentComment IS NULL " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findActiveParentCommentsByPostId(@Param("postId") Long postId);

    /**
     * 특정 부모 댓글의 모든 활성 대댓글 조회
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 대댓글 목록
     */
    @Query("SELECT c FROM Comment c " +
           "WHERE c.parentComment.id = :parentCommentId " +
           "AND c.deleted = false " +
           "ORDER BY c.createdAt ASC")
    List<Comment> findActiveChildCommentsByParentId(@Param("parentCommentId") Long parentCommentId);

    /**
     * 게시글의 댓글 개수 조회 (활성 댓글만, 대댓글 포함)
     *
     * @param postId 게시글 ID
     * @return 댓글 개수
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
           "WHERE c.post.id = :postId " +
           "AND c.deleted = false")
    long countActiveCommentsByPostId(@Param("postId") Long postId);

    /**
     * 댓글 ID로 댓글 조회 (삭제 여부 상관없이)
     *
     * @param id 댓글 ID
     * @return 댓글
     */
    Optional<Comment> findById(Long id);

    /**
     * 게시글의 모든 댓글 조회 (삭제된 것 포함)
     * 부모 → 자식 순서로 조회하기 위해 parentComment 오름차순, 생성 시간 오름차순
     *
     * @param postId 게시글 ID
     * @return 댓글 목록
     */
    @Query("SELECT c FROM Comment c " +
           "WHERE c.post.id = :postId " +
           "ORDER BY c.parentComment.id ASC, c.createdAt ASC")
    List<Comment> findAllCommentsByPostId(@Param("postId") Long postId);

    /**
     * 사용자가 작성한 활성 댓글 개수
     *
     * @param userId 사용자 ID
     * @return 댓글 개수
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
           "WHERE c.user.id = :userId " +
           "AND c.deleted = false")
    long countActiveCommentsByUserId(@Param("userId") Long userId);

    /**
     * 부모 댓글 삭제 시 대댓글 존재 여부 확인
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 활성 대댓글 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comment c " +
           "WHERE c.parentComment.id = :parentCommentId " +
           "AND c.deleted = false")
    boolean existsActiveChildCommentsByParentId(@Param("parentCommentId") Long parentCommentId);
}

