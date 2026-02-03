package com.sobunsobun.backend.application.post;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.PostReport;
import com.sobunsobun.backend.domain.ReportStatus;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.post.PostReportDto;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.PostReportRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 게시글 신고 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostReportService {

    private final PostReportRepository postReportRepository;
    private final GroupPostRepository groupPostRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 신고 생성
     */
    public PostReportDto.Response createReport(Long userId, PostReportDto.CreateRequest request) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 게시글 조회
        GroupPost post = groupPostRepository.findById(request.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다"));

        // 중복 신고 확인
        Optional<PostReport> existingReport = postReportRepository.findByUserIdAndPostId(userId, request.getPostId());
        if (existingReport.isPresent()) {
            throw new IllegalArgumentException("이미 이 게시글을 신고했습니다");
        }

        // 자신의 게시글 신고 방지
        if (post.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("자신의 게시글은 신고할 수 없습니다");
        }

        // 신고 생성
        PostReport report = PostReport.builder()
                .user(user)
                .post(post)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PostReport savedReport = postReportRepository.save(report);

        log.info("게시글 신고 생성 - 신고자: {}, 게시글: {}, 사유: {}", userId, post.getId(), request.getReason());

        return convertToResponse(savedReport);
    }

    /**
     * 신고 조회
     */
    @Transactional(readOnly = true)
    public PostReportDto.Response getReport(Long reportId) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        return convertToResponse(report);
    }

    /**
     * 사용자의 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PostReportDto.ListResponse> getMyReports(Long userId, Pageable pageable) {
        return postReportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToListResponse);
    }

    /**
     * 신고 상태별 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<PostReportDto.ListResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return postReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::convertToListResponse);
    }

    /**
     * 특정 게시글의 모든 신고 조회
     */
    @Transactional(readOnly = true)
    public Page<PostReportDto.ListResponse> getPostReports(Long postId, Pageable pageable) {
        java.util.List<PostReportDto.ListResponse> content = postReportRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::convertToListResponse)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        long total = postReportRepository.countByPostId(postId);
        return new org.springframework.data.domain.PageImpl<>(content, pageable, total);
    }

    /**
     * 신고 상태 업데이트 (관리자)
     */
    public PostReportDto.Response updateReportStatus(Long reportId, Long adminId, PostReportDto.UpdateStatusRequest request) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        report.setStatus(request.getStatus());
        report.setResolution(request.getResolution());
        report.setHandledByAdminId(adminId);
        report.setHandledAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        PostReport updatedReport = postReportRepository.save(report);

        log.info("신고 상태 업데이트 - 신고ID: {}, 새 상태: {}", reportId, request.getStatus());

        return convertToResponse(updatedReport);
    }

    /**
     * 신고 삭제
     */
    public void deleteReport(Long reportId) {
        PostReport report = postReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        postReportRepository.delete(report);

        log.info("신고 삭제 - 신고ID: {}", reportId);
    }

    /**
     * 게시글별 신고 통계
     */
    @Transactional(readOnly = true)
    public PostReportDto.StatisticsResponse getPostReportStatistics(Long postId) {
        long totalReports = postReportRepository.countByPostId(postId);
        long pendingReports = postReportRepository.countByPostIdAndStatus(postId, ReportStatus.PENDING);
        long reviewingReports = postReportRepository.countByPostIdAndStatus(postId, ReportStatus.REVIEWING);
        long resolvedReports = postReportRepository.countByPostIdAndStatus(postId, ReportStatus.RESOLVED);

        return PostReportDto.StatisticsResponse.builder()
                .postId(postId)
                .totalReports(totalReports)
                .pendingReports(pendingReports)
                .reviewingReports(reviewingReports)
                .resolvedReports(resolvedReports)
                .build();
    }

    /**
     * PostReport를 Response DTO로 변환
     */
    private PostReportDto.Response convertToResponse(PostReport report) {
        return PostReportDto.Response.builder()
                .id(report.getId())
                .postId(report.getPost().getId())
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

    /**
     * PostReport를 ListResponse DTO로 변환
     */
    private PostReportDto.ListResponse convertToListResponse(PostReport report) {
        return PostReportDto.ListResponse.builder()
                .id(report.getId())
                .postId(report.getPost().getId())
                .postTitle(report.getPost().getTitle())
                .userId(report.getUser().getId())
                .userName(report.getUser().getNickname())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .handledAt(report.getHandledAt())
                .build();
    }
}
