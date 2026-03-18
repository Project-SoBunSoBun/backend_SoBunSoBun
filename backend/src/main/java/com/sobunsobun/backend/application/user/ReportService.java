package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.GroupPost;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.domain.UserReport;
import com.sobunsobun.backend.dto.user.UserReportRequest;
import com.sobunsobun.backend.repository.GroupPostRepository;
import com.sobunsobun.backend.repository.UserReportRepository;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.exception.BusinessException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 신고 비즈니스 로직 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final UserRepository userRepository;
    private final UserReportRepository userReportRepository;
    private final GroupPostRepository groupPostRepository;

    /**
     * 유저 신고
     *
     * @param reporterId   신고자 ID
     * @param targetUserId 신고 대상 ID
     * @param request      신고 사유 및 내용
     * @throws BusinessException REPORT_SELF_NOT_ALLOWED  - 자기 자신 신고 시도
     * @throws BusinessException REPORT_TARGET_NOT_FOUND  - 대상 사용자 없음
     * @throws BusinessException REPORT_ALREADY_REPORTED  - 해당 게시글에서 이미 신고한 사용자
     */
    @Transactional
    public void reportUser(Long reporterId, Long targetUserId, UserReportRequest request) {
        if (reporterId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.REPORT_SELF_NOT_ALLOWED);
        }

        if (userReportRepository.existsByReporterIdAndTargetUserIdAndGroupPostId(
                reporterId, targetUserId, request.getGroupPostId())) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_REPORTED);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND));

        GroupPost groupPost = groupPostRepository.findById(request.getGroupPostId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        UserReport report = UserReport.of(reporter, targetUser, groupPost, request.getReason(), request.getDescription());
        userReportRepository.save(report);

        log.info("유저 신고 완료 - reporterId: {}, targetUserId: {}, groupPostId: {}, reason: {}",
                reporterId, targetUserId, request.getGroupPostId(), request.getReason());
    }
}
