package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.support.InquiryTypeListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 고객 지원 공통 정보 컨트롤러
 *
 * 담당 기능:
 * - 문의 유형 목록 조회
 * - 버그 신고 유형 목록 조회
 *
 * 특징:
 * - 인증 불필요한 공통 정보 제공
 * - 모든 사용자가 접근 가능
 *
 * TODO: SupportService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 공통 정보", description = "문의/버그 유형 API")
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    // TODO: SupportService 주입 및 구현
    // private final SupportService supportService;

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

            // TODO: Service 호출로 교체
            // InquiryTypeListResponse types = supportService.getInquiryTypes();

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

            // TODO: Service 호출로 교체
            // InquiryTypeListResponse types = supportService.getBugTypes();

            // 임시 응답 (InquiryTypeListResponse 재사용)
            InquiryTypeListResponse types = InquiryTypeListResponse.builder()
                    .build();

            log.info("✅ 버그 신고 유형 목록 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(types));
        } catch (Exception e) {
            log.error("❌ 버그 신고 유형 목록 조회 중 오류 발생", e);
            throw e;
        }
    }
}

