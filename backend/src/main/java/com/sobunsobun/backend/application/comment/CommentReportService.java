package com.sobunsobun.backend.application.comment;

import com.sobunsobun.backend.domain.Comment;
import com.sobunsobun.backend.domain.CommentReport;
import com.sobunsobun.backend.domain.ReportStatus;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.comment.CommentReportDto;
import com.sobunsobun.backend.repository.CommentReportRepository;
import com.sobunsobun.backend.repository.CommentRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentReportService {

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentReportDto.Response createReport(Long userId, CommentReportDto.CreateRequest2 request) {
        log.info("=== 댓글 신고 요청 ===");
        log.info("userId: {}", userId);
        log.info("commentId: {}", request.getCommentId());
        log.info("reason: {}", request.getReason());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Comment comment = commentRepository.findById(request.getCommentId())
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다"));

        if (comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("자신의 댓글은 신고할 수 없습니다");
        }

        Optional<CommentReport> existingReport = commentReportRepository.findByUserIdAndCommentId(userId, request.getCommentId());
        if (existingReport.isPresent()) {
            throw new IllegalArgumentException("이미 이 댓글을 신고했습니다");
        }

        CommentReport report = CommentReport.builder()
                .user(user)
                .comment(comment)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        CommentReport savedReport = commentReportRepository.save(report);
        log.info("댓글 신고 저장 완료 - reportId: {}", savedReport.getId());

        return convertToResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public CommentReportDto.Response getReport(Long reportId) {
        CommentReport report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        return convertToResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<CommentReportDto.ListResponse> getMyReports(Long userId, Pageable pageable) {
        return commentReportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToListResponse);
    }

    @Transactional(readOnly = true)
    public Page<CommentReportDto.ListResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        // 상태별 조회 (필요시 repository에 메서드 추가)
        throw new UnsupportedOperationException("상태별 조회는 추후 구현");
    }

    @Transactional(readOnly = true)
    public Page<CommentReportDto.ListResponse> getCommentReports(Long commentId, Pageable pageable) {
        // 댓글별 조회 (필요시 repository에 메서드 추가)
        throw new UnsupportedOperationException("댓글별 조회는 추후 구현");
    }

    public CommentReportDto.Response updateReportStatus(Long reportId, Long adminId, CommentReportDto.UpdateStatusRequest request) {
        CommentReport report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        report.setStatus(request.getStatus());
        report.setResolution(request.getResolution());
        report.setHandledAt(LocalDateTime.now());

        CommentReport updatedReport = commentReportRepository.save(report);
        return convertToResponse(updatedReport);
    }

    public void deleteReport(Long reportId) {
        CommentReport report = commentReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        commentReportRepository.delete(report);
    }

    private CommentReportDto.Response convertToResponse(CommentReport report) {
        return CommentReportDto.Response.builder()
                .id(report.getId())
                .commentId(report.getComment().getId())
                .userId(report.getUser().getId())
                .userName(report.getUser().getNickname())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .resolution(report.getResolution())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .handledAt(report.getHandledAt())
                .build();
    }

    private CommentReportDto.ListResponse convertToListResponse(CommentReport report) {
        return CommentReportDto.ListResponse.builder()
                .id(report.getId())
                .commentId(report.getComment().getId())
                .commentContent(report.getComment().getContent())
                .userId(report.getUser().getId())
                .userName(report.getUser().getNickname())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .handledAt(report.getHandledAt())
                .build();
    }
}
