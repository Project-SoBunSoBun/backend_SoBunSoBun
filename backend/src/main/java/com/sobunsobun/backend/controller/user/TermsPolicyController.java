package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.TermsService;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.terms.TermsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 약관 및 정책 컨트롤러
 *
 * 담당 기능:
 * - 서비스 이용약관 조회
 * - 개인정보처리방침 조회
 * - 약관 버전 관리
 *
 * 특징:
 * - 인증 불필요한 공개 API
 * - 모든 사용자가 접근 가능
 */
@Slf4j
@Tag(name = "User - 약관 및 정책", description = "서비스 약관 API")
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsPolicyController {

    private final TermsService termsService;

    @Operation(summary = "서비스 이용약관 조회 (최신 버전)")
    @GetMapping("/service")
    public ResponseEntity<ApiResponse<TermsResponse>> getServiceTerms() {
        TermsResponse terms = termsService.getLatestTerms("SERVICE");
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    @Operation(summary = "개인정보처리방침 조회 (최신 버전)")
    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<TermsResponse>> getPrivacyPolicy() {
        TermsResponse terms = termsService.getLatestTerms("PRIVACY");
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    @Operation(summary = "위치기반서비스 이용약관 조회 (최신 버전)")
    @GetMapping("/location")
    public ResponseEntity<ApiResponse<TermsResponse>> getLocationTerms() {
        TermsResponse terms = termsService.getLatestTerms("LOCATION");
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    @Operation(summary = "약관 버전 목록 조회")
    @GetMapping("/{type}/versions")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getTermsVersions(
            @PathVariable @Parameter(description = "약관 유형 (SERVICE, PRIVACY, LOCATION)") String type) {
        List<TermsResponse> versions = termsService.getTermsVersions(type);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @Operation(summary = "특정 버전 약관 조회")
    @GetMapping("/{type}/versions/{version}")
    public ResponseEntity<ApiResponse<TermsResponse>> getTermsByVersion(
            @PathVariable @Parameter(description = "약관 유형") String type,
            @PathVariable @Parameter(description = "버전 (예: 1.0.0)") String version) {
        TermsResponse terms = termsService.getTermsByVersion(type, version);
        return ResponseEntity.ok(ApiResponse.success(terms));
    }
}
