package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.PostReport;
import com.sobunsobun.backend.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    /**
     * 사용자가 특정 게시글에 대해 이미 신고했는지 확인
     */
    Optional<PostReport> findByUserIdAndPostId(Long userId, Long postId);

    /**
     * 특정 게시글에 대한 모든 신고 조회
     */
    List<PostReport> findByPostIdOrderByCreatedAtDesc(Long postId);

    /**
     * 사용자가 한 신고 목록 (페이징)
     */
    Page<PostReport> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 신고 상태별 신고 목록 (페이징)
     */
    Page<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * 특정 상태의 신고 개수
     */
    long countByStatus(ReportStatus status);

    /**
     * 특정 게시글의 신고 개수
     */
    long countByPostId(Long postId);

    /**
     * 특정 게시글에 대한 신고 상태별 개수
     */
    long countByPostIdAndStatus(Long postId, ReportStatus status);
}
