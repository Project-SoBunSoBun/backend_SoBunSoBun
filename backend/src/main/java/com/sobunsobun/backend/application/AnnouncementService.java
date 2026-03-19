package com.sobunsobun.backend.application;

import com.sobunsobun.backend.domain.Announcement;
import com.sobunsobun.backend.dto.announcement.AnnouncementDetailResponse;
import com.sobunsobun.backend.dto.announcement.AnnouncementListItemResponse;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공지사항 비즈니스 로직 서비스
 *
 * 담당 기능:
 * - 공지사항 목록 조회 (페이지네이션)
 * - 공지사항 상세 조회
 * - 조회수 증가
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    /**
     * 공지사항 목록 조회
     *
     * @param pageable 페이지네이션 정보
     * @return 공지사항 목록 페이지 응답
     */
    public PageResponse<AnnouncementListItemResponse> getAnnouncements(Pageable pageable) {
        log.info(" 공지사항 목록 조회 시작 - 페이지: {}, 사이즈: {}", pageable.getPageNumber(), pageable.getPageSize());

        try {
            log.debug(" Repository findAll 호출 전");
            Page<Announcement> announcementPage = announcementRepository.findAll(pageable);
            log.debug(" Repository findAll 호출 완료 - 총 요소: {}", announcementPage.getTotalElements());

            if (announcementPage.getTotalElements() == 0) {
                log.warn(" 공지사항이 없습니다");
            }

            // Announcement 엔티티를 AnnouncementListItemResponse DTO로 변환
            log.debug(" DTO 변환 시작 - 컨텐츠 개수: {}", announcementPage.getContent().size());
            var content = announcementPage.getContent().stream()
                    .map(announcement -> {
                        try {
                            log.debug("  - ID: {}, Title: {}, IsPinned: {}",
                                announcement.getId(),
                                announcement.getTitle(),
                                announcement.getIsPinned());
                            return AnnouncementListItemResponse.builder()
                                    .id(announcement.getId())
                                    .title(announcement.getTitle())
                                    .category(announcement.getCategory())
                                    .isPinned(announcement.getIsPinned())
                                    .createdAt(announcement.getCreatedAt())
                                    .build();
                        } catch (Exception e) {
                            log.error(" DTO 변환 중 오류 - ID: {}", announcement.getId(), e);
                            throw new RuntimeException("DTO 변환 실패: " + e.getMessage(), e);
                        }
                    })
                    .toList();
            log.debug(" DTO 변환 완료 - {} 개", content.size());

            // PageInfo 생성
            log.debug(" PageInfo 생성 중");
            PageResponse.PageInfo pageInfo = PageResponse.PageInfo.builder()
                    .number(announcementPage.getNumber())
                    .size(announcementPage.getSize())
                    .totalElements(announcementPage.getTotalElements())
                    .totalPages(announcementPage.getTotalPages())
                    .first(announcementPage.isFirst())
                    .last(announcementPage.isLast())
                    .hasNext(announcementPage.hasNext())
                    .hasPrevious(announcementPage.hasPrevious())
                    .build();
            log.debug(" PageInfo 생성 완료");

            log.debug(" PageResponse 생성 중");
            PageResponse<AnnouncementListItemResponse> response = PageResponse.<AnnouncementListItemResponse>builder()
                    .content(content)
                    .page(pageInfo)
                    .build();
            log.debug(" PageResponse 생성 완료");

            log.info(" 공지사항 목록 조회 완료 - 총 개수: {}, 현재 페이지: {}",
                    announcementPage.getTotalElements(), announcementPage.getNumber());

            return response;
        } catch (Exception e) {
            log.error(" 공지사항 목록 조회 중 오류 발생", e);
            log.error("오류 메시지: {}", e.getMessage());
            log.error("오류 클래스: {}", e.getClass().getName());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "공지사항 목록 조회에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 공지사항 상세 조회
     *
     * @param id 공지사항 ID
     * @return 공지사항 상세 응답
     */
    @Transactional
    public AnnouncementDetailResponse getAnnouncementDetail(Long id) {
        log.info(" 공지사항 상세 조회 시작 - ID: {}", id);

        try {
            Announcement announcement = announcementRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn(" 공지사항을 찾을 수 없음 - ID: {}", id);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 공지사항을 찾을 수 없습니다.");
                    });

            // 조회수 증가
            announcement.incrementViewCount();
            announcementRepository.save(announcement);
            log.debug("조회수 증가 완료 - ID: {}, 조회수: {}", id, announcement.getViewCount());

            log.info(" 공지사항 상세 조회 완료 - ID: {}", id);

            return AnnouncementDetailResponse.builder()
                    .id(announcement.getId())
                    .title(announcement.getTitle())
                    .content(announcement.getContent())
                    .category(announcement.getCategory())
                    .isPinned(announcement.getIsPinned())
                    .viewCount(announcement.getViewCount())
                    .createdAt(announcement.getCreatedAt())
                    .updatedAt(announcement.getUpdatedAt())
                    .build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error(" 공지사항 상세 조회 중 오류 발생 - ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 상세 조회에 실패했습니다.");
        }
    }

    /**
     * 공지사항 총 개수 조회 (디버그용)
     *
     * @return 공지사항 총 개수
     */
    public long getAnnouncementCount() {
        return announcementRepository.count();
    }

    /**
     * 공지사항 원본 페이지 조회 (디버그용)
     *
     * @param pageable 페이지네이션 정보
     * @return 원본 페이지
     */
    public Page<Announcement> getAnnouncementsRaw(Pageable pageable) {
        return announcementRepository.findAll(pageable);
    }
}
