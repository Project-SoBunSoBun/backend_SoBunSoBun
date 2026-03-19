package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.dto.account.*;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 사용자 계정 관리 컨트롤러
 *
 * 담당 기능:
 * - 계정 정보 조회
 * - 로그아웃
 * - 회원 탈퇴
 * - 탈퇴 사유 목록 조회
 */
@Slf4j
@Tag(name = "User - 계정 관리", description = "로그아웃/탈퇴 API")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyAccountController {

    private final UserService userService;

    /**
     * 계정 정보 조회
     *
     * 조회 항목:
     * - 이메일
     * - 가입일
     * - 최근 로그인 일시
     * - 계정 상태
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 계정 정보
     */
    @Operation(
        summary = "계정 정보 조회",
        description = "사용자의 계정 정보(이메일, 가입일 등)를 조회합니다."
    )
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<AccountInfoResponse>> getAccountInfo(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 계정 정보 조회 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // AccountInfoResponse accountInfo = accountService.getAccountInfo(principal.id());

            // 임시 응답
            AccountInfoResponse accountInfo = AccountInfoResponse.builder()
                    .build();

            log.info(" 계정 정보 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(accountInfo));
        } catch (Exception e) {
            log.error(" 계정 정보 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 로그아웃
     *
     * 처리 사항:
     * - Refresh Token 무효화
     * - FCM 토큰 비활성화 (선택)
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 로그아웃 요청 (FCM 토큰 포함)
     * @return 로그아웃 성공 메시지
     */
    @Operation(
        summary = "로그아웃",
        description = "사용자를 로그아웃 처리하고 Refresh Token을 무효화합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            Authentication authentication,
            @Valid @RequestBody(required = false) LogoutRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 로그아웃 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // LogoutResponse response = accountService.logout(principal.id(), request);

            // 임시 응답
            LogoutResponse response = LogoutResponse.builder()
                    .message("로그아웃되었습니다.")
                    .build();

            log.info(" 로그아웃 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 로그아웃 처리 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 탈퇴 사유 목록 조회
     *
     * @return 탈퇴 사유 목록
     */
    @Operation(
        summary = "탈퇴 사유 목록 조회",
        description = "회원 탈퇴 시 선택할 수 있는 사유 목록을 조회합니다."
    )
    @GetMapping("/withdraw/reasons")
    public ResponseEntity<ApiResponse<WithdrawReasonListResponse>> getWithdrawReasons() {
        try {
            log.info(" 탈퇴 사유 목록 조회 요청");

            List<WithdrawReasonListResponse.WithdrawReasonItem> reasons = Arrays.asList(
                    new WithdrawReasonListResponse.WithdrawReasonItem("RARELY_USED", "잘 사용하지 않아요"),
                    new WithdrawReasonListResponse.WithdrawReasonItem("NO_NEARBY_POSTS", "근처에 게시글이 없어요"),
                    new WithdrawReasonListResponse.WithdrawReasonItem("INCONVENIENT", "사용이 불편해요"),
                    new WithdrawReasonListResponse.WithdrawReasonItem("PRIVACY_CONCERN", "개인정보가 걱정돼요"),
                    new WithdrawReasonListResponse.WithdrawReasonItem("BAD_EXPERIENCE", "나쁜 경험이 있었어요"),
                    new WithdrawReasonListResponse.WithdrawReasonItem("OTHER", "기타")
            );

            WithdrawReasonListResponse response = WithdrawReasonListResponse.builder()
                    .reasons(reasons)
                    .build();

            log.info(" 탈퇴 사유 목록 조회 완료 - {} 개", reasons.size());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 탈퇴 사유 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 회원 탈퇴
     *
     * 처리 사항:
     * - User 상태를 DELETED로 변경
     * - withdrawn_at, reactivatable_at 기록
     * - 탈퇴 사유 저장
     *
     * 재가입 제한:
     * - 탈퇴 후 90일간 재가입 불가
     * - 90일 경과 후 재가입 시 새 계정으로 생성
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 탈퇴 요청 (사유, 동의 여부)
     * @return 탈퇴 처리 결과
     */
    @Operation(
        summary = "회원 탈퇴",
        description = "회원 탈퇴를 처리합니다. 탈퇴 후 90일간 재가입이 제한됩니다."
    )
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WithdrawResponse>> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 회원 탈퇴 요청 - 사용자 ID: {}", principal.id());

            WithdrawResponse response = userService.withdrawUser(principal.id(), request);

            log.info(" 회원 탈퇴 완료 - 사용자 ID: {}, 탈퇴 일시: {}, 재가입 가능 일시: {}",
                    principal.id(), response.getWithdrawnAt(), response.getReactivatableAt());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error(" 회원 탈퇴 처리 중 오류 발생 - 사용자 ID: {}",
                    ((JwtUserPrincipal) authentication.getPrincipal()).id(), e);
            throw e;
        }
    }
}

