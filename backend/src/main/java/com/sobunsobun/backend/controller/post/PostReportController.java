package com.sobunsobun.backend.controller.post;

import com.sobunsobun.backend.application.post.PostReportService;
import com.sobunsobun.backend.domain.ReportStatus;
import com.sobunsobun.backend.dto.post.PostReportDto;
import com.sobunsobun.backend.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 신고 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/posts/reports")
@RequiredArgsConstructor
@Tag(name = "Post Report", description = "게시글 신고 API")
public class PostReportController {

    private final PostReportService postReportService;

    /**
     * 게시글 신고 생성
     * POST /api/v1/posts/reports
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다")
    public ResponseEntity<PostReportDto.Response> createReport(
            @Valid @RequestBody PostReportDto.CreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        PostReportDto.Response response = postReportService.createReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 신고 조회
     * GET /api/v1/posts/reports/{reportId}
     */
    @GetMapping("/{reportId}")
    @Operation(summary = "신고 조회", description = "신고 상세 정보를 조회합니다")
    public ResponseEntity<PostReportDto.Response> getReport(
            @PathVariable Long reportId) {
        PostReportDto.Response response = postReportService.getReport(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 신고 목록 조회
     * GET /api/v1/posts/reports/my/list
     */
    @GetMapping("/my/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 신고 목록", description = "현재 사용자가 한 신고 목록을 조회합니다")
    public ResponseEntity<Page<PostReportDto.ListResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostReportDto.ListResponse> response = postReportService.getMyReports(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 신고 상태별 목록 조회 (관리자)
     * GET /api/v1/posts/reports/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "신고 상태별 목록 (관리자)", description = "신고 상태별 신고 목록을 조회합니다 (관리자만)")
    public ResponseEntity<Page<PostReportDto.ListResponse>> getReportsByStatus(
            @RequestParam ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostReportDto.ListResponse> response = postReportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 게시글의 신보 목록 조회
     * GET /api/v1/posts/{postId}/reports
     */
    @GetMapping("/post/{postId}")
    @Operation(summary = "게시글별 신고 목록", description = "특정 게시글에 대한 모든 신고를 조회합니다")
    public ResponseEntity<Page<PostReportDto.ListResponse>> getPostReports(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostReportDto.ListResponse> response = postReportService.getPostReports(postId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 신고 상태 업데이트 (관리자)
     * PATCH /api/v1/posts/reports/{reportId}/status
     */
    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "신고 상태 업데이트 (관리자)", description = "신고 상태를 업데이트합니다 (관리자만)")
    public ResponseEntity<PostReportDto.Response> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody PostReportDto.UpdateStatusRequest request) {
        Long adminId = SecurityUtil.getCurrentUserId();
        PostReportDto.Response response = postReportService.updateReportStatus(reportId, adminId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 신고 삭제
     * DELETE /api/v1/posts/reports/{reportId}
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "신고 삭제 (관리자)", description = "신고를 삭제합니다 (관리자만)")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long reportId) {
        postReportService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글별 신고 통계
     * GET /api/v1/posts/{postId}/reports/statistics
     */
    @GetMapping("/post/{postId}/statistics")
    @Operation(summary = "게시글별 신고 통계", description = "게시글의 신고 통계를 조회합니다")
    public ResponseEntity<PostReportDto.StatisticsResponse> getPostReportStatistics(
            @PathVariable Long postId) {
        PostReportDto.StatisticsResponse response = postReportService.getPostReportStatistics(postId);
        return ResponseEntity.ok(response);
    }
}
