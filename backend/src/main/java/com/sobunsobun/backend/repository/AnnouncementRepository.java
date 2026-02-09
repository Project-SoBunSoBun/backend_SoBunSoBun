package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     * - 상단 고정 공지사항이 먼저 나오고, 그 다음 최신순으로 정렬
     *
     * @param pageable 페이지네이션 정보
     * @return 공지사항 페이지
     */
    @Query("SELECT a FROM Announcement a ORDER BY a.isPinned DESC, a.createdAt DESC")
    Page<Announcement> findAllWithPinnedFirst(Pageable pageable);

    /**
     * 공지사항 상세 조회 (작성자 정보 포함)
     *
     * @param id 공지사항 ID
     * @return 공지사항 상세 정보
     */
    Optional<Announcement> findById(Long id);
}
