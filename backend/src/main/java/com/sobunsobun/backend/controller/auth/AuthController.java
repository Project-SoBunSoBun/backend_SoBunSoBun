package com.sobunsobun.backend.controller.auth;

import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.dto.auth.*;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 인증/인가 관련 REST API 컨트롤러
 *
 * 담당 기능:
 * - 카카오 OAuth 로그인/회원가입
 * - 애플 OAuth 로그인/회원가입
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
     * 1단계 (Apple): Apple 로그인 검증 - REST API 방식
     *
     * - iOS 앱에서 Apple SDK로 받은 authorization code 또는 id_token 전달
     * - 사용자 정보 검증 후 임시 로그인 토큰 발급 (10분 만료)
     * - JWT는 발급하지 않음 (이용약관 동의 후 발급)
     *
     * @param request Apple authorization code 또는 id_token
     * @return 사용자 정보 및 임시 로그인 토큰
     */
    @Operation(summary = "Apple 토큰 검증",
               description = "Apple authorization code 또는 id_token으로 사용자 정보를 검증하고 임시 로그인 토큰을 발급합니다.")
    @PostMapping("/verify/apple-token")
    public ResponseEntity<KakaoVerifyResponse> verifyAppleToken(
            @RequestBody @Validated AppleLoginRequest request) {

        log.info("Apple 토큰 검증 요청");
        KakaoVerifyResponse response = authService.verifyAppleToken(request.getCode(), request.getIdToken());
        log.info("Apple 토큰 검증 완료 - 이메일: {}, 신규사용자: {}",
                response.getEmail(), response.isNewUser());

        return ResponseEntity.ok(response);
    }

    /**
     * Apple OAuth 콜백 엔드포인트
     *
     * - Apple에서 POST로 authorization_code를 전달
     * - 웹 기반 로그인 플로우에서 사용
     * - form-data로 code, id_token 수신
     *
     * @param code Apple authorization code
     * @param idToken Apple id_token
     * @return 사용자 정보 및 임시 로그인 토큰
     */
    @Operation(summary = "Apple OAuth 콜백",
               description = "Apple에서 POST로 전달하는 OAuth 콜백 엔드포인트입니다.")
    @PostMapping("/callback/apple")
    public ResponseEntity<KakaoVerifyResponse> appleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "id_token", required = false) String idToken) {

        log.info("Apple OAuth 콜백 수신");
        KakaoVerifyResponse response = authService.verifyAppleToken(code, idToken);
        log.info("Apple OAuth 콜백 처리 완료 - 이메일: {}, 신규사용자: {}",
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
     * Apple 계정 연결 해제 (Revoke)
     *
     * - Authorization 헤더에 Bearer 액세스 토큰 필수
     * - DB에서 Apple refresh_token 조회 후 Apple /auth/revoke API 호출
     * - authorization_code 교환 경로(웹/서버사이드)로 로그인한 경우에만 가능
     * - iOS 앱 id_token 직접 전달 방식은 Apple 앱에서 직접 연결 해제 필요
     *
     * @param principal 인증된 사용자 정보 (JWT에서 추출)
     * @return 204 No Content (성공 시 본문 없음)
     */
    @Operation(summary = "Apple 계정 연결 해제",
               description = "Apple ID 연결을 해제합니다. " +
                       "DB에 저장된 Apple refresh_token으로 /auth/revoke API를 호출합니다. " +
                       "Authorization: Bearer {accessToken} 헤더가 필요합니다.")
    @PostMapping("/revoke/apple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeAppleAccount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        log.info("Apple 계정 연결 해제 요청 - 사용자 ID: {}", principal.id());
        authService.revokeAppleAccount(principal.id());
        log.info("Apple 계정 연결 해제 완료 - 사용자 ID: {}", principal.id());

        return ResponseEntity.noContent().build();
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
