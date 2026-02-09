package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.BugReport;
import com.sobunsobun.backend.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 버그 신고 Repository
 */
@Repository
public interface BugReportRepository extends JpaRepository<BugReport, Long> {

    /**
     * 사용자의 버그 신고 목록 조회 (페이징)
     */
    Page<BugReport> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자의 버그 신고 목록 조회 (상태별)
     */
    Page<BugReport> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);

    /**
     * 일정 기간의 버그 신고 목록 조회
     */
    List<BugReport> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 버그 타입별 개수 조회
     */
    long countByTypeCode(String typeCode);

    /**
     * 사용자별 버그 신고 개수 조회
     */
    long countByUser(User user);

    /**
     * 상태별 버그 신고 개수 조회
     */
    long countByStatus(String status);
}
