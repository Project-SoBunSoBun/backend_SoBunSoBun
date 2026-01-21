package com.sobunsobun.backend.controller.user;

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

    @Operation(summary = "서비스 이용약관 조회")
    @GetMapping("/service")
    public ResponseEntity<ApiResponse<TermsResponse>> getServiceTerms() {
        try {
            log.info("📄 서비스 이용약관 조회 요청");

            TermsResponse terms = TermsResponse.builder()
                    .type("SERVICE")
                    .version("1.0.0")
                    .title("서비스 이용약관")
                    .content("서비스 이용약관 내용...")
                    .build();

            log.info("✅ 서비스 이용약관 조회 완료 - 버전: {}", terms.getVersion());

            return ResponseEntity.ok(ApiResponse.success(terms));
        } catch (Exception e) {
            log.error("❌ 서비스 이용약관 조회 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(summary = "개인정보처리방침 조회")
    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<TermsResponse>> getPrivacyPolicy() {
        try {
            log.info("📄 개인정보처리방침 조회 요청");

            TermsResponse terms = TermsResponse.builder()
                    .type("PRIVACY")
                    .version("1.0.0")
                    .title("개인정보처리방침")
                    .content("개인정보처리방침 내용...")
                    .build();

            log.info("✅ 개인정보처리방침 조회 완료 - 버전: {}", terms.getVersion());

            return ResponseEntity.ok(ApiResponse.success(terms));
        } catch (Exception e) {
            log.error("❌ 개인정보처리방침 조회 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(summary = "약관 버전 목록 조회")
    @GetMapping("/{type}/versions")
    public ResponseEntity<ApiResponse<List<TermsResponse>>> getTermsVersions(
            @PathVariable @Parameter(description = "약관 유형") String type) {
        try {
            log.info("📄 약관 버전 목록 조회 요청 - 유형: {}", type);

            List<TermsResponse> versions = List.of();

            log.info("✅ 약관 버전 목록 조회 완료 - 유형: {}", type);

            return ResponseEntity.ok(ApiResponse.success(versions));
        } catch (Exception e) {
            log.error("❌ 약관 버전 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(summary = "특정 버전 약관 조회")
    @GetMapping("/{type}/versions/{version}")
    public ResponseEntity<ApiResponse<TermsResponse>> getTermsByVersion(
            @PathVariable String type,
            @PathVariable String version) {
        try {
            log.info("📄 특정 버전 약관 조회 요청 - 유형: {}, 버전: {}", type, version);

            TermsResponse terms = TermsResponse.builder()
                    .type(type)
                    .version(version)
                    .build();

            log.info("✅ 특정 버전 약관 조회 완료 - 유형: {}, 버전: {}", type, version);

            return ResponseEntity.ok(ApiResponse.success(terms));
        } catch (Exception e) {
            log.error("❌ 특정 버전 약관 조회 중 오류 발생", e);
            throw e;
        }
    }
}
