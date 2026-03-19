package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.AnnouncementService;
import com.sobunsobun.backend.dto.announcement.AnnouncementDetailResponse;
import com.sobunsobun.backend.dto.announcement.AnnouncementListItemResponse;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 공지사항 컨트롤러
 *
 * 담당 기능:
 * - 공지사항 목록 조회 (페이지네이션)
 * - 공지사항 상세 조회
 *
 * 특징:
 * - 인증 불필요 (공개 API)
 * - 모든 사용자가 접근 가능
 */
@Slf4j
@Tag(name = "User - 공지사항", description = "공지사항 조회 API")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 공지사항 목록 조회
     *
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 공지사항 목록
     */
    @Operation(
        summary = "공지사항 목록 조회",
        description = "공지사항 목록을 페이지네이션하여 조회합니다. 최신순으로 정렬됩니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementListItemResponse>>> getAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String[] sort) {

        try {
            // 페이지 사이즈 제한 (최대 100)
            if (size > 100) {
                size = 100;
            }
            if (size < 1) {
                size = 1;
            }

            // Pageable 생성 - sort 파라미터 무시하고 항상 기본값 사용
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
            );

            log.info(" 공지사항 목록 조회 요청 - 페이지: {}, 사이즈: {}", page, size);

            PageResponse<AnnouncementListItemResponse> announcements = announcementService.getAnnouncements(pageable);

            log.info(" 공지사항 목록 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(announcements));
        } catch (Exception e) {
            log.error(" 공지사항 목록 조회 중 오류 발생", e);
            log.error("오류 메시지: {}", e.getMessage());
            log.error("오류 클래스: {}", e.getClass().getName());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 공지사항 상세 조회
     *
     * @param id 공지사항 ID
     * @return 공지사항 상세 정보
     */
    @Operation(
        summary = "공지사항 상세 조회",
        description = "특정 공지사항의 상세 내용을 조회합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementDetailResponse>> getAnnouncementDetail(
            @PathVariable @Parameter(description = "공지사항 ID") Long id) {
        try {
            log.info(" 공지사항 상세 조회 요청 - ID: {}", id);

            AnnouncementDetailResponse announcement = announcementService.getAnnouncementDetail(id);

            log.info(" 공지사항 상세 조회 완료 - ID: {}", id);

            return ResponseEntity.ok(ApiResponse.success(announcement));
        } catch (Exception e) {
            log.error(" 공지사항 상세 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 디버그용: 데이터베이스 상태 확인
     */
    @GetMapping("/debug/count")
    public ResponseEntity<String> debugCount() {
        try {
            long count = announcementService.getAnnouncementCount();
            log.info(" 공지사항 총 개수: {}", count);
            return ResponseEntity.ok("공지사항 총 개수: " + count);
        } catch (Exception e) {
            log.error(" 디버그 조회 실패", e);
            e.printStackTrace();
            return ResponseEntity.ok("오류: " + e.getMessage());
        }
    }

    /**
     * 디버그용: 원본 페이지 객체 확인
     */
    @GetMapping("/debug/page")
    public ResponseEntity<String> debugPage() {
        try {
            log.info(" 디버그 페이지 조회 시작");
            org.springframework.data.domain.PageRequest pageable =
                org.springframework.data.domain.PageRequest.of(0, 20,
                    org.springframework.data.domain.Sort.Direction.DESC, "createdAt");

            org.springframework.data.domain.Page<com.sobunsobun.backend.domain.Announcement> page =
                announcementService.getAnnouncementsRaw(pageable);

            StringBuilder sb = new StringBuilder();
            sb.append("총 개수: ").append(page.getTotalElements()).append("\n");
            sb.append("현재 페이지: ").append(page.getNumber()).append("\n");
            sb.append("페이지당 크기: ").append(page.getSize()).append("\n");
            sb.append("콘텐츠 크기: ").append(page.getContent().size()).append("\n");

            page.getContent().forEach(ann ->
                sb.append("- ID: ").append(ann.getId())
                  .append(", Title: ").append(ann.getTitle())
                  .append(", IsPinned: ").append(ann.getIsPinned())
                  .append("\n")
            );

            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            log.error(" 디버그 페이지 조회 실패", e);
            e.printStackTrace();
            return ResponseEntity.ok("오류: " + e.getMessage());
        }
    }
}
