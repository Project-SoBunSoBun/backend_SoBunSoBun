package com.sobunsobun.backend.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.application.file.FileStorageService;
import com.sobunsobun.backend.domain.BugReport;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.support.BugReportRequest;
import com.sobunsobun.backend.dto.support.BugReportResponse;
import com.sobunsobun.backend.repository.BugReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 버그 신고 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BugReportService {

    private final BugReportRepository bugReportRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    // 이메일 검증 정규식
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    /**
     * 버그 신고 제출
     * @param request 버그 신고 요청
     * @param user 로그인한 사용자
     * @return 버그 신고 응답
     */
    public BugReportResponse submitBugReport(BugReportRequest request, User user) {
        log.info("🐛 [submitBugReport] 버그 신고 제출 시작 - userId: {}, typeCode: {}", user.getId(), request.getTypeCode());

        try {
            // 필드 검증
            if (request.getContent() == null || request.getContent().isBlank()) {
                log.warn("⚠️ [submitBugReport] content 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("버그 내용은 필수입니다.");
            }
            if (request.getTypeCode() == null || request.getTypeCode().isBlank()) {
                log.warn("⚠️ [submitBugReport] typeCode 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("버그 유형은 필수입니다.");
            }
            if (request.getReplyEmail() == null || request.getReplyEmail().isBlank()) {
                log.warn("⚠️ [submitBugReport] replyEmail 필드가 null 또는 빈 문자열입니다");
                throw new IllegalArgumentException("답변 받을 이메일은 필수입니다.");
            }

            // 이메일 형식 검증
            if (!isValidEmail(request.getReplyEmail())) {
                log.warn("⚠️ [submitBugReport] 잘못된 이메일 형식: '{}'", request.getReplyEmail());
                throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + request.getReplyEmail());
            }
            log.info("✅ [submitBugReport] 이메일 형식 검증 통과: '{}'", request.getReplyEmail());

            // 스크린샷 파일 저장
            List<String> imageUrls = new ArrayList<>();
            if (request.getScreenshots() != null && !request.getScreenshots().isEmpty()) {
                log.info("📷 [submitBugReport] 스크린샷 {} 개 저장 시작", request.getScreenshots().size());
                for (MultipartFile screenshot : request.getScreenshots()) {
                    if (screenshot != null && !screenshot.isEmpty()) {
                        try {
                            String imageUrl = fileStorageService.saveImage(screenshot);
                            if (imageUrl != null) {
                                imageUrls.add(imageUrl);
                                log.info("✅ [submitBugReport] 스크린샷 저장 성공: {}", imageUrl);
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ [submitBugReport] 스크린샷 저장 실패: {}", e.getMessage());
                        }
                    }
                }
            }

            // 이미지 URL을 JSON 배열로 변환
            String imageUrlsJson = null;
            if (!imageUrls.isEmpty()) {
                imageUrlsJson = objectMapper.writeValueAsString(imageUrls);
                log.info("📦 [submitBugReport] 이미지 URL JSON: {}", imageUrlsJson);
            }

            // 디바이스 정보를 JSON으로 변환
            String deviceInfoJson = null;
            if (request.getDeviceInfo() != null) {
                deviceInfoJson = objectMapper.writeValueAsString(request.getDeviceInfo());
                log.info("📱 [submitBugReport] 디바이스 정보 JSON: {}", deviceInfoJson);
            }

            // 버그 신고 엔티티 생성
            BugReport bugReport = BugReport.builder()
                    .user(user)
                    .typeCode(request.getTypeCode())
                    .content(request.getContent())
                    .replyEmail(request.getReplyEmail())
                    .imageUrls(imageUrlsJson)
                    .deviceInfo(deviceInfoJson)
                    .status("RECEIVED")
                    .build();

            // 저장
            BugReport savedBugReport = bugReportRepository.save(bugReport);
            log.info("✅ [submitBugReport] 버그 신고 저장 완료 - bugReportId: {}", savedBugReport.getId());

            return BugReportResponse.builder()
                    .bugReportId(savedBugReport.getId())
                    .status(savedBugReport.getStatus())
                    .message("버그 신고가 접수되었습니다. 빠른 시일 내에 확인하여 답변 드리겠습니다.")
                    .createdAt(savedBugReport.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("❌ [submitBugReport] 버그 신고 제출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("버그 신고 제출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자의 버그 신고 목록 조회
     * @param user 사용자
     * @param pageable 페이징
     * @return 버그 신고 목록
     */
    @Transactional(readOnly = true)
    public Page<BugReport> getUserBugReports(User user, Pageable pageable) {
        log.info("📋 [getUserBugReports] 버그 신고 목록 조회 - userId: {}", user.getId());
        return bugReportRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * 버그 신고 상세 조회
     * @param bugReportId 버그 신고 ID
     * @param user 로그인한 사용자
     * @return 버그 신고
     */
    @Transactional(readOnly = true)
    public BugReport getBugReport(Long bugReportId, User user) {
        log.info("🔍 [getBugReport] 버그 신고 상세 조회 - bugReportId: {}, userId: {}", bugReportId, user.getId());
        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new IllegalArgumentException("버그 신고를 찾을 수 없습니다."));

        // 신고자만 조회 가능 또는 관리자
        if (!bugReport.getUser().getId().equals(user.getId())) {
            log.warn("⚠️ [getBugReport] 권한 없음 - userId: {}, reportUser: {}", user.getId(), bugReport.getUser().getId());
            throw new IllegalArgumentException("자신의 버그 신고만 조회할 수 있습니다.");
        }

        return bugReport;
    }

    /**
     * 스크린샷 이미지 URL 목록 조회
     * @param bugReport 버그 신고
     * @return 이미지 URL 목록
     */
    @Transactional(readOnly = true)
    public List<String> getBugReportImageUrls(BugReport bugReport) {
        if (bugReport.getImageUrls() == null || bugReport.getImageUrls().isBlank()) {
            return new ArrayList<>();
        }

        try {
            List<String> urls = objectMapper.readValue(bugReport.getImageUrls(), List.class);
            log.info("📷 [getBugReportImageUrls] 이미지 URL 조회 - count: {}", urls.size());
            return urls;
        } catch (Exception e) {
            log.warn("⚠️ [getBugReportImageUrls] 이미지 URL 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 이메일 형식 검증
     * @param email 검증할 이메일 주소
     * @return 유효한 이메일이면 true, 아니면 false
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
