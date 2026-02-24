package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.support.BugReportService;
import com.sobunsobun.backend.application.support.InquiryService;
import com.sobunsobun.backend.domain.BugReport;
import com.sobunsobun.backend.domain.Inquiry;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.support.BugReportRequest;
import com.sobunsobun.backend.dto.support.BugReportResponse;
import com.sobunsobun.backend.dto.support.InquiryRequest;
import com.sobunsobun.backend.dto.support.InquiryResponse;
import com.sobunsobun.backend.dto.support.InquiryTypeListResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 고객 지원 컨트롤러
 *
 * 담당 기능:
 * - 문의 유형 목록 조회 (공개)
 * - 버그 신고 유형 목록 조회 (공개)
 * - 1:1 문의 제출 (스크린샷 첨부 가능) - 인증 필요
 * - 문의 목록 및 상세 조회 - 인증 필요
 * - 버그 신고 제출 (스크린샷 + 디바이스 정보) - 인증 필요
 * - 버그 신고 목록 및 상세 조회 - 인증 필요
 */
@Slf4j
@Tag(name = "User - 고객 지원", description = "문의/버그 신고 API")
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final InquiryService inquiryService;
    private final BugReportService bugReportService;

    // ======================== 공개 API (인증 불필요) ========================

    /**
     * 문의 유형 목록 조회
     *
     * @return 문의 유형 목록
     */
    @Operation(
        summary = "문의 유형 목록 조회",
        description = "1:1 문의 시 선택할 수 있는 유형 목록을 조회합니다."
    )
    @GetMapping("/inquiry-types")
    public ResponseEntity<ApiResponse<InquiryTypeListResponse>> getInquiryTypes() {
        try {
            log.info("📋 문의 유형 목록 조회 요청");

            // 임시 응답
            InquiryTypeListResponse types = InquiryTypeListResponse.builder()
                    .build();

            log.info("✅ 문의 유형 목록 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            log.error("❌ 문의 유형 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 버그 신고 유형 목록 조회
     *
     * @return 버그 신고 유형 목록
     */
    @Operation(
        summary = "버그 신고 유형 목록 조회",
        description = "버그 신고 시 선택할 수 있는 유형 목록을 조회합니다."
    )
    @GetMapping("/bug-types")
    public ResponseEntity<ApiResponse<InquiryTypeListResponse>> getBugTypes() {
        try {
            log.info("🐛 버그 신고 유형 목록 조회 요청");

            // 임시 응답
            InquiryTypeListResponse types = InquiryTypeListResponse.builder()
                    .build();

            log.info("✅ 버그 신고 유형 목록 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            log.error("❌ 버그 신고 유형 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    // ======================== 1:1 문의 API (인증 필요) ========================

    /**
     * 1:1 문의 제출 (스크린샷 첨부 가능)
     *
     * @param typeCode 문의 유형 코드
     * @param content 문의 내용
     * @param replyEmail 답변 받을 이메일
     * @param screenshots 스크린샷 파일 (선택)
     * @param principal 인증 사용자
     * @return 문의 응답
     */
    @PostMapping(value = "/inquiry", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "1:1 문의 제출",
            description = "스크린샷 첨부가 가능한 1:1 문의를 제출합니다. 최대 5개의 스크린샷(jpg/png/webp, 각 5MB)을 첨부할 수 있습니다."
    )
    public ResponseEntity<InquiryResponse> submitInquiry(
            @RequestParam("typeCode") String typeCode,
            @RequestParam("content") String content,
            @RequestParam("replyEmail") String replyEmail,
            @RequestParam(value = "screenshots", required = false) List<MultipartFile> screenshots,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("📝 [submitInquiry API] 1:1 문의 제출 - userId: {}", principal.id());
        log.info("📝 [submitInquiry API] 요청 파라미터 검증");
        log.info("  - typeCode: '{}' (null: {})", typeCode, typeCode == null);
        log.info("  - content: '{}' (null: {}, length: {})",
                content, content == null, content != null ? content.length() : 0);
        log.info("  - replyEmail: '{}' (null: {})", replyEmail, replyEmail == null);
        log.info("  - screenshots: {} (null: {})",
                screenshots != null ? screenshots.size() : 0, screenshots == null);

        // 스크린샷 상세 로그
        if (screenshots != null && !screenshots.isEmpty()) {
            for (int i = 0; i < screenshots.size(); i++) {
                MultipartFile file = screenshots.get(i);
                log.info("  - screenshot[{}]: name='{}', originalFilename='{}', size={}, contentType='{}', empty={}",
                        i, file.getName(), file.getOriginalFilename(), file.getSize(),
                        file.getContentType(), file.isEmpty());
            }
        }

        // InquiryRequest 객체 생성
        InquiryRequest request = InquiryRequest.builder()
                .typeCode(typeCode)
                .content(content)
                .replyEmail(replyEmail)
                .screenshots(screenshots)
                .build();

        User user = new User();
        user.setId(principal.id());

        InquiryResponse response = inquiryService.submitInquiry(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 문의 목록 조회
     *
     * @param principal 인증 사용자
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 문의 목록
     */
    @GetMapping("/inquiries")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "사용자의 문의 목록 조회",
            description = "현재 로그인한 사용자가 제출한 1:1 문의 목록을 조회합니다 (페이징)."
    )
    public ResponseEntity<Page<Inquiry>> getUserInquiries(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📋 [getUserInquiries API] 문의 목록 조회 - userId: {}, page: {}, size: {}",
                principal.id(), page, size);

        User user = new User();
        user.setId(principal.id());

        Pageable pageable = PageRequest.of(page, size);
        Page<Inquiry> inquiries = inquiryService.getUserInquiries(user, pageable);

        return ResponseEntity.ok(inquiries);
    }

    /**
     * 문의 상세 조회
     *
     * @param inquiryId 문의 ID
     * @param principal 인증 사용자
     * @return 문의 상세 정보
     */
    @GetMapping("/inquiry/{inquiryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "문의 상세 조회",
            description = "특정 1:1 문의의 상세 정보를 조회합니다. 첨부된 스크린샷 URL도 포함됩니다."
    )
    public ResponseEntity<Inquiry> getInquiry(
            @Parameter(description = "문의 ID", example = "1")
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("🔍 [getInquiry API] 문의 상세 조회 - inquiryId: {}, userId: {}",
                inquiryId, principal.id());

        User user = new User();
        user.setId(principal.id());

        Inquiry inquiry = inquiryService.getInquiry(inquiryId, user);
        return ResponseEntity.ok(inquiry);
    }

    /**
     * 문의 첨부 이미지 목록 조회
     *
     * @param inquiryId 문의 ID
     * @param principal 인증 사용자
     * @return 이미지 URL 목록
     */
    @GetMapping("/inquiry/{inquiryId}/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "문의 첨부 이미지 목록 조회",
            description = "특정 문의에 첨부된 스크린샷의 URL 목록을 조회합니다."
    )
    public ResponseEntity<List<String>> getInquiryImages(
            @Parameter(description = "문의 ID", example = "1")
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("📷 [getInquiryImages API] 문의 이미지 조회 - inquiryId: {}", inquiryId);

        User user = new User();
        user.setId(principal.id());

        Inquiry inquiry = inquiryService.getInquiry(inquiryId, user);
        List<String> imageUrls = inquiryService.getInquiryImageUrls(inquiry);

        return ResponseEntity.ok(imageUrls);
    }

    // ======================== 버그 신고 API (인증 필요) ========================

    /**
     * 버그 신고 제출 (스크린샷 첨부 가능)
     *
     * @param typeCode 버그 유형 코드
     * @param content 버그 내용
     * @param screenshots 스크린샷 파일 (선택)
     * @param principal 인증 사용자
     * @return 버그 신고 응답
     */
    @PostMapping(value = "/bug-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "버그 신고 제출",
            description = "스크린샷을 첨부할 수 있는 버그 신고를 제출합니다. 최대 5개의 스크린샷(jpg/png/webp, 각 5MB)을 첨부할 수 있습니다."
    )
    public ResponseEntity<BugReportResponse> submitBugReport(
            @RequestParam String typeCode,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> screenshots,
            @RequestParam(required = false) String deviceInfo,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("🐛 [submitBugReport API] 버그 신고 제출 - userId: {}", principal.id());
        log.info("🐛 [submitBugReport API] 요청 데이터 - typeCode: {}, content: {}, deviceInfo: {}",
                typeCode, content, deviceInfo);

        // BugReportRequest 객체 생성
        BugReportRequest request = BugReportRequest.builder()
                .typeCode(typeCode)
                .content(content)
                .screenshots(screenshots)
                .deviceInfo(deviceInfo)
                .build();

        User user = new User();
        user.setId(principal.id());

        BugReportResponse response = bugReportService.submitBugReport(request, user);
        return ResponseEntity.ok(response);
    }


    /**
     * 사용자의 버그 신고 목록 조회
     *
     * @param principal 인증 사용자
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @return 버그 신고 목록
     */
    @GetMapping("/bug-reports")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "사용자의 버그 신고 목록 조회",
            description = "현재 로그인한 사용자가 제출한 버그 신고 목록을 조회합니다 (페이징)."
    )
    public ResponseEntity<Page<BugReport>> getUserBugReports(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📋 [getUserBugReports API] 버그 신고 목록 조회 - userId: {}, page: {}, size: {}",
                principal.id(), page, size);

        User user = new User();
        user.setId(principal.id());

        Pageable pageable = PageRequest.of(page, size);
        Page<BugReport> bugReports = bugReportService.getUserBugReports(user, pageable);

        return ResponseEntity.ok(bugReports);
    }

    /**
     * 버그 신고 상세 조회
     *
     * @param bugReportId 버그 신고 ID
     * @param principal 인증 사용자
     * @return 버그 신고 상세 정보
     */
    @GetMapping("/bug-report/{bugReportId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "버그 신고 상세 조회",
            description = "특정 버그 신고의 상세 정보를 조회합니다. 첨부된 스크린샷 URL과 디바이스 정보도 포함됩니다."
    )
    public ResponseEntity<BugReport> getBugReport(
            @Parameter(description = "버그 신고 ID", example = "1")
            @PathVariable Long bugReportId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("🔍 [getBugReport API] 버그 신고 상세 조회 - bugReportId: {}, userId: {}",
                bugReportId, principal.id());

        User user = new User();
        user.setId(principal.id());

        BugReport bugReport = bugReportService.getBugReport(bugReportId, user);
        return ResponseEntity.ok(bugReport);
    }

    /**
     * 버그 신고 첨부 이미지 목록 조회
     *
     * @param bugReportId 버그 신고 ID
     * @param principal 인증 사용자
     * @return 이미지 URL 목록
     */
    @GetMapping("/bug-report/{bugReportId}/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "버그 신고 첨부 이미지 목록 조회",
            description = "특정 버그 신고에 첨부된 스크린샷의 URL 목록을 조회합니다."
    )
    public ResponseEntity<List<String>> getBugReportImages(
            @Parameter(description = "버그 신고 ID", example = "1")
            @PathVariable Long bugReportId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("📷 [getBugReportImages API] 버그 신고 이미지 조회 - bugReportId: {}", bugReportId);

        User user = new User();
        user.setId(principal.id());

        BugReport bugReport = bugReportService.getBugReport(bugReportId, user);
        List<String> imageUrls = bugReportService.getBugReportImageUrls(bugReport);

        return ResponseEntity.ok(imageUrls);
    }
}

