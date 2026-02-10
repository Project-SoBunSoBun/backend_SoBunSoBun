package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Inquiry;
import com.sobunsobun.backend.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 1:1 문의 Repository
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * 사용자의 문의 목록 조회 (페이징)
     */
    Page<Inquiry> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 사용자의 문의 목록 조회 (상태별)
     */
    Page<Inquiry> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);

    /**
     * 일정 기간의 문의 목록 조회
     */
    List<Inquiry> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 문의 타입별 개수 조회
     */
    long countByTypeCode(String typeCode);

    /**
     * 사용자별 문의 개수 조회
     */
    long countByUser(User user);
}
