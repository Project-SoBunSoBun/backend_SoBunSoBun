package com.sobunsobun.backend.controller.auth;

import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.dto.auth.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 인증/인가 관련 REST API 컨트롤러
 *
 * 담당 기능:
 * - 카카오 OAuth 로그인/회원가입
 * - JWT 토큰 발급 및 갱신
 * - 이용약관 동의 기반 회원가입 플로우
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 1단계: 카카오 토큰 검증 및 이메일 확인
     *
     * - 카카오 액세스 토큰으로 사용자 정보 조회
     * - 이메일 검증 후 임시 로그인 토큰 발급 (10분 만료)
     * - JWT는 발급하지 않음 (이용약관 동의 후 발급)
     *
     * @param request 카카오 액세스 토큰 요청
     * @return 사용자 정보 및 임시 로그인 토큰
     */
    @Operation(summary = "카카오 토큰 검증",
               description = "카카오 액세스 토큰으로 사용자 정보를 검증하고 임시 로그인 토큰을 발급합니다람쥐")
    @PostMapping("/verify/kakao-token")
    public ResponseEntity<KakaoVerifyResponse> verifyKakaoToken(
            @RequestBody @Validated KakaoTokenLoginRequest request) {

        log.info("카카오 토큰 검증 요청");
        KakaoVerifyResponse response = authService.verifyKakaoToken(request.getAccessToken());
        log.info("카카오 토큰 검증 완료 - 이메일: {}, 신규사용자: {}",
                response.getEmail(), response.isNewUser());

        return ResponseEntity.ok(response);
    }

    /**
     * 2단계: 이용약관 동의 후 회원가입 완료
     *
     * - 임시 로그인 토큰 검증
     * - 필수 약관 동의 여부 확인
     * - 사용자 등록/업데이트 후 JWT 토큰 발급
     *
     * @param request 약관 동의 정보 및 임시 토큰
     * @return JWT 액세스/리프레시 토큰 4개 정보
     */
    @Operation(summary = "회원가입 완료",
            description = "이용약관 동의 후 회원가입을 완료하고 JWT 토큰을 발급합니다. " +
                    "※ 주의: 이미 가입된 사용자는 kakao-token 검증 단계에서 isNewUser=false로 표시됩니다.")

    @PostMapping("/complete-signup")
    public ResponseEntity<AuthResponse> completeSignup(
            @RequestBody @Validated TermsAgreementRequest request) {

        log.info("회원가입 완료 요청 - 서비스약관: {}, 개인정보: {}, 마케팅: {}",
                request.isServiceTermsAgreed(),
                request.isPrivacyPolicyAgreed(),
                request.isMarketingOptionalAgreed());



        AuthResponse response = authService.completeSignupWithTerms(request);
        log.info("회원가입 완료 - 사용자 ID: {}", response.getUser().getId());

        return ResponseEntity.ok(response);
    }


    /**
     * JWT 리프레시 토큰으로 액세스 토큰 갱신
     *
     * - 리프레시 토큰 검증 (만료, 서명 확인)
     * - 새로운 액세스 토큰만 발급 (리프레시 토큰은 재발급 안함)
     * - 사용자 활성 상태 확인
     *
     * @param request 리프레시 토큰 요청
     * @return 새로운 액세스 토큰 정보
     */
    @Operation(summary = "액세스 토큰 갱신",
               description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    @PostMapping("/token/refresh")
    public ResponseEntity<AccessOnlyResponse> refreshAccessToken(
            @RequestBody @Validated RefreshTokenRequest request) {

        log.info("액세스 토큰 갱신 요청");
        AccessOnlyResponse response = authService.refreshAccessOnly(request.getRefreshToken());
        log.info("액세스 토큰 갱신 완료");

        return ResponseEntity.ok(response);
    }
}
