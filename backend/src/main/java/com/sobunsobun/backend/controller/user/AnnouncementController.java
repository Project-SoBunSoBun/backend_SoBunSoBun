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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            log.info("📢 공지사항 목록 조회 요청 - 페이지: {}", pageable.getPageNumber());

            PageResponse<AnnouncementListItemResponse> announcements = announcementService.getAnnouncements(pageable);

            log.info("✅ 공지사항 목록 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(announcements));
        } catch (Exception e) {
            log.error("❌ 공지사항 목록 조회 중 오류 발생", e);
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
            log.info("📢 공지사항 상세 조회 요청 - ID: {}", id);

            AnnouncementDetailResponse announcement = announcementService.getAnnouncementDetail(id);

            log.info("✅ 공지사항 상세 조회 완료 - ID: {}", id);

            return ResponseEntity.ok(ApiResponse.success(announcement));
        } catch (Exception e) {
            log.error("❌ 공지사항 상세 조회 중 오류 발생", e);
            throw e;
        }
    }
}

