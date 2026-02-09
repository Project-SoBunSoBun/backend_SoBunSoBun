package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 공지사항 저장소
 * JPA를 사용한 Announcement 엔티티 데이터 접근 계층
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * 공지사항 목록 조회 (페이지네이션)
     * Pageable로 정렬 처리
     *
     * @param pageable 페이지네이션 정보
     * @return 공지사항 페이지
     */
    Page<Announcement> findAll(Pageable pageable);

    /**
     * 공지사항 상세 조회
     *
     * @param id 공지사항 ID
     * @return 공지사항 상세 정보
     */
    Optional<Announcement> findById(Long id);
}
