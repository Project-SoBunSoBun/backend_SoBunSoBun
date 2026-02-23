package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    Optional<CommentReport> findByUserIdAndCommentId(Long userId, Long commentId);

    Page<CommentReport> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByCommentId(Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    /**
     * 특정 사용자가 한 모든 댓글 신고 삭제 (회원탈퇴용)
     */
    void deleteByUserId(Long userId);

    /**
     * 특정 댓글에 대한 모든 신고 삭제 (댓글 삭제 시)
     */
    void deleteByCommentId(Long commentId);
}
