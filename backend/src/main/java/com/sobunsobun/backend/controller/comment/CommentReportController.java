package com.sobunsobun.backend.controller.comment;

import com.sobunsobun.backend.application.comment.CommentReportService;
import com.sobunsobun.backend.domain.ReportStatus;
import com.sobunsobun.backend.dto.comment.CommentReportDto;
import com.sobunsobun.backend.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/comments/reports")
@RequiredArgsConstructor
@Tag(name = "Comment Report", description = "댓글 신고 API")
public class CommentReportController {

    private final CommentReportService commentReportService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다")
    public ResponseEntity<CommentReportDto.Response> createReport(
            @Valid @RequestBody CommentReportDto.CreateRequest2 request) {
        Long userId = SecurityUtil.getCurrentUserId();
        CommentReportDto.Response response = commentReportService.createReport(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "신고 조회", description = "신고 상세 정보를 조회합니다")
    public ResponseEntity<CommentReportDto.Response> getReport(@PathVariable Long reportId) {
        CommentReportDto.Response response = commentReportService.getReport(reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 신고 목록", description = "현재 사용자가 한 신고 목록을 조회합니다")
    public ResponseEntity<Page<CommentReportDto.ListResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentReportDto.ListResponse> response = commentReportService.getMyReports(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "신고 상태 업데이트 (관리자)", description = "신고 상태를 업데이트합니다")
    public ResponseEntity<CommentReportDto.Response> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody CommentReportDto.UpdateStatusRequest request) {
        Long adminId = SecurityUtil.getCurrentUserId();
        CommentReportDto.Response response = commentReportService.updateReportStatus(reportId, adminId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "신고 삭제 (관리자)", description = "신고를 삭제합니다")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        commentReportService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }
}
