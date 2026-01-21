package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.dto.support.*;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 고객 지원 컨트롤러
 *
 * 담당 기능:
 * - 1:1 문의 제출 및 내역 조회
 * - 버그 신고 제출
 *
 * TODO: SupportService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 고객 지원", description = "1:1 문의 및 버그 신고 API")
@RestController
@RequestMapping("/api/me/support")
@RequiredArgsConstructor
public class MySupportController {

    // TODO: SupportService 주입 및 구현
    // private final SupportService supportService;

    /**
     * 1:1 문의 제출
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 문의 요청 (유형, 제목, 내용, 이미지 등)
     * @return 문의 제출 결과
     */
    @Operation(
        summary = "1:1 문의 제출",
        description = "고객 문의를 제출합니다. 이미지 첨부가 가능합니다."
    )
    @PostMapping("/inquiries")
    public ResponseEntity<ApiResponse<InquiryResponse>> submitInquiry(
            Authentication authentication,
            @Valid @RequestBody InquiryRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📬 1:1 문의 제출 요청 - 사용자 ID: {}, 유형: {}", principal.id(), request);

            // TODO: Service 호출로 교체
            // InquiryResponse response = supportService.submitInquiry(principal.id(), request);

            // 임시 응답
            InquiryResponse response = InquiryResponse.builder()
                    .message("문의가 접수되었습니다.")
                    .build();

            log.info("✅ 1:1 문의 제출 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ 1:1 문의 제출 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 내 문의 내역 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 문의 내역 목록
     */
    @Operation(
        summary = "내 문의 내역 조회",
        description = "사용자가 제출한 1:1 문의 내역을 페이지네이션하여 조회합니다."
    )
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<PageResponse<InquiryResponse>>> getMyInquiries(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📋 문의 내역 조회 요청 - 사용자 ID: {}, 페이지: {}", principal.id(), pageable.getPageNumber());

            // TODO: Service 호출로 교체
            // PageResponse<InquiryResponse> response = supportService.getMyInquiries(principal.id(), pageable);

            // 임시 응답
            PageResponse<InquiryResponse> response = new PageResponse<>();

            log.info("✅ 문의 내역 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ 문의 내역 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 버그 신고 제출
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 버그 신고 요청 (유형, 제목, 내용, 스크린샷 등)
     * @return 버그 신고 제출 결과
     */
    @Operation(
        summary = "버그 신고 제출",
        description = "앱에서 발견한 버그를 신고합니다. 스크린샷 첨부가 가능합니다."
    )
    @PostMapping("/bugs")
    public ResponseEntity<ApiResponse<BugReportResponse>> submitBugReport(
            Authentication authentication,
            @Valid @RequestBody BugReportRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🐛 버그 신고 제출 요청 - 사용자 ID: {}, 유형: {}", principal.id(), request);

            // TODO: Service 호출로 교체
            // BugReportResponse response = supportService.submitBugReport(principal.id(), request);

            // 임시 응답
            BugReportResponse response = BugReportResponse.builder()
                    .message("버그 신고가 접수되었습니다.")
                    .build();

            log.info("✅ 버그 신고 제출 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ 버그 신고 제출 중 오류 발생", e);
            throw e;
        }
    }
}

