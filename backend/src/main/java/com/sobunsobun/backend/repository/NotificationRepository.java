package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 알림 내역 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순, 페이징) - 특정 타입 제외
     */
    Page<Notification> findByUserIdAndTypeNotOrderByCreatedAtDesc(Long userId, String type, Pageable pageable);

    /**
     * 사용자의 읽지 않은 알림 개수
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 사용자의 읽지 않은 알림 개수 (특정 타입 제외)
     */
    long countByUserIdAndIsReadFalseAndTypeNot(Long userId, String type);

    /**
     * 사용자의 읽지 않은 알림 목록
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 모든 알림 삭제 (회원탈퇴용)
     */
    void deleteByUserId(Long userId);
}
