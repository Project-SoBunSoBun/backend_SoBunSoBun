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
}
