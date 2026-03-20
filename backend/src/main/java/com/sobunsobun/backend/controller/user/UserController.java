package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.application.user.MyProfileService;
import com.sobunsobun.backend.support.response.ApiResponse;
import com.sobunsobun.backend.dto.user.NicknameRequest;
import com.sobunsobun.backend.dto.user.ProfileUpdateRequest;
import com.sobunsobun.backend.dto.user.UserProfileResponse;
import com.sobunsobun.backend.dto.account.WithdrawRequest;
import com.sobunsobun.backend.dto.account.WithdrawResponse;
import com.sobunsobun.backend.dto.account.WithdrawalReasonResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 사용자 관리 REST API 컨트롤러
 *
 * 담당 기능:
 * - 닉네임 중복 확인 (공개 API)
 * - 사용자 프로필 관리 (인증 필요)
 *
 * 특징:
 * - 모든 API는 검증 된 입력값으로 처리
 * - 닉네임 정규화를 통한 데이터 일관성 보장
 * - 명확한 로깅으로 감시 가능성 향상
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User - 기본", description = "사용자 기본 관리 API")
public class UserController {

    private final UserService userService;
    private final MyProfileService myProfileService;

    private static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]+$";

    /**
     * 다른 사용자 프로필 조회 API (공개)
     *
     * 이미지나 닉네임 클릭 시 해당 유저 프로필을 확인하는 기능
     * 인증 없이 호출 가능합니다.
     *
     * 응답 정보:
     * - userId: 사용자 ID
     * - nickname: 닉네임
     * - profileImageUrl: 프로필 이미지 URL
     * - mannerScore: 매너 점수 (0.00 ~ 5.00)
     * - participationCount: 공동구매 참여 횟수
     * - hostCount: 방장(개설) 횟수
     * - postCount: 작성한 글 수
     * - mannerTags: 받은 매너 평가 태그 목록 (상위 5개)
     * - posts: 작성한 게시글 목록 (게시글 제목, 상태, 금액, 지역, 마감일 등)
     *
     * @param userId 조회할 사용자 ID (경로 변수)
     * @return 사용자 프로필 정보
     */
    @Operation(
        summary = "다른 사용자 프로필 조회",
        description = "이미지나 닉네임 클릭 시 해당 유저의 프로필 정보를 조회합니다. 인증 없이 호출 가능합니다."
    )
    @GetMapping("/{userId}/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @Parameter(description = "조회할 사용자 ID", example = "1")
            @PathVariable
            Long userId) {

        try {
            log.info(" 다른 사용자 프로필 조회 요청 - userId: {}", userId);

            UserProfileResponse profile = myProfileService.getUserProfile(userId);

            log.info(" 다른 사용자 프로필 조회 완료 - userId: {}, nickname: {}", userId, profile.getNickname());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", profile
            ));
        } catch (Exception e) {
            log.error(" 다른 사용자 프로필 조회 중 오류 발생 - userId: {}", userId, e);
            throw e;
        }
    }

    /**
     * 닉네임으로 다른 사용자 프로필 조회 API (공개)
     *
     * 닉네임을 클릭했을 때 해당 유저 프로필을 확인하는 기능
     * 인증 없이 호출 가능합니다.
     *
     * 응답 정보:
     * - userId: 사용자 ID
     * - nickname: 닉네임
     * - profileImageUrl: 프로필 이미지 URL
     * - mannerScore: 매너 점수 (0.00 ~ 5.00)
     * - participationCount: 공동구매 참여 횟수
     * - hostCount: 방장(개설) 횟수
     * - postCount: 작성한 글 수
     * - mannerTags: 받은 매너 평가 태그 목록 (상위 5개)
     * - posts: 작성한 게시글 목록 (게시글 제목, 상태, 금액, 지역, 마감일 등)
     *
     * @param nickname 조회할 사용자 닉네임 (쿼리 파라미터)
     * @return 사용자 프로필 정보
     */
    @Operation(
        summary = "다른 사용자 프로필 조회 (닉네임으로)",
        description = "닉네임으로 해당 유저의 프로필 정보를 조회합니다. 인증 없이 호출 가능합니다."
    )
    @GetMapping("/profile/by-nickname")
    public ResponseEntity<Map<String, Object>> getUserProfileByNickname(
            @Parameter(description = "조회할 사용자 닉네임", example = "행복한고래")
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            String nickname) {

        try {
            log.info(" 닉네임으로 다른 사용자 프로필 조회 요청 - nickname: {}", nickname);

            UserProfileResponse profile = myProfileService.getUserProfileByNickname(nickname);

            log.info(" 닉네임으로 다른 사용자 프로필 조회 완료 - nickname: {}, userId: {}", nickname, profile.getUserId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", profile
            ));
        } catch (Exception e) {
            log.error(" 닉네임으로 다른 사용자 프로필 조회 중 오류 발생 - nickname: {}", nickname, e);
            throw e;
        }
    }

    /**
     * 닉네임 중복 확인 API (공개)
     *
     * 회원가입/프로필 설정 시 닉네임 사용 가능 여부를 확인합니다.
     *
     * 검증 규칙:
     * - 1~8자 이내
     * - 한글, 영문, 숫자만 허용
     * - 특수문자, 공백 불허
     *
     * @param nickname 확인할 닉네임 (쿼리 파라미터)
     * @return 닉네임과 사용 가능 여부
     */
    @Operation(
        summary = "닉네임 중복 확인",
        description = "닉네임이 사용 가능한지 확인합니다. 회원가입 없이 호출 가능합니다."
    )
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNicknameAvailability(
            @Parameter(description = "확인할 닉네임 (1-8자, 한글/영문/숫자만)", example = "행복한고래")
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            @Size(max = 8, message = "닉네임은 최대 8자입니다.")
            @Pattern(regexp = NICKNAME_PATTERN, message = "닉네임은 한글/영문/숫자만 가능합니다.")
            String nickname) {

        try {
            log.info(" 닉네임 중복 확인 요청: {}", nickname);

            // 닉네임 정규화 (공백 제거, 소문자 변환 등)
            String normalizedNickname = userService.normalizeNickname(nickname);
            boolean isAvailable = userService.isNicknameAvailable(normalizedNickname);

            log.info(" 닉네임 중복 확인 완료: {} -> 정규화: {}, 사용가능: {}",
                    nickname, normalizedNickname, isAvailable);

            return ResponseEntity.ok(Map.of(
                    "nickname", nickname,
                    "normalizedNickname", normalizedNickname,
                    "available", isAvailable
            ));
        } catch (Exception e) {
            log.error(" 닉네임 확인 중 오류 발생: {}", nickname, e);
            throw e;
        }
    }

    /**
     * 내 닉네임 변경 API (인증 필요)
     *
     * JWT 토큰으로 인증된 사용자의 닉네임을 변경합니다.
     *
     * 주의사항:
     * - 중복 확인을 먼저 수행하는 것을 권장
     * - 변경 후 프로필 캐시 갱신 필요
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param request 새로운 닉네임 정보
     * @return 변경된 닉네임 정보
     */
    @Operation(
        summary = "내 닉네임 변경",
        description = "인증된 사용자의 닉네임을 변경합니다. JWT 토큰 필수입니다."
    )
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateMyNickname(
            @Parameter(hidden = true) // Swagger에서 숨김 (JWT에서 자동 추출)
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "새로운 닉네임 정보")
            @RequestBody @Valid NicknameRequest request) {

        Long userId = principal.id();
        String newNickname = request.nickname();

        log.info(" 닉네임 변경 요청 - 사용자 ID: {}, 새 닉네임: {}", userId, newNickname);

        String normalizedNickname = userService.normalizeNickname(newNickname);
        userService.updateUserNickname(userId, normalizedNickname);

        log.info(" 닉네임 변경 완료 - 사용자 ID: {}, 변경된 닉네임: {}", userId, normalizedNickname);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 프로필 업데이트 API (닉네임 + 프로필 이미지) - 인증 필요
     *
     * 회원가입 완료 또는 프로필 수정 시 호출합니다.
     * 닉네임과 프로필 이미지를 함께 업데이트할 수 있습니다.
     *
     * 요청 형식: multipart/form-data
     * - nickname: 새로운 닉네임 (필수)
     * - profileImage: 프로필 이미지 파일 (선택, jpg/png/webp, 5MB 이하)
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param nickname 새로운 닉네임
     * @param profileImage 프로필 이미지 파일 (선택적)
     * @return 업데이트 결과
     */
    @Operation(
        summary = "프로필 업데이트 (닉네임 + 이미지)",
        description = "닉네임과 프로필 이미지를 업데이트합니다. 이미지를 보내지 않으면 기존 이미지가 유지됩니다."
    )
    @PatchMapping(value = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal,

            @Parameter(description = "새로운 닉네임 (1-8자, 한글/영문/숫자)")
            @RequestParam
            @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
            @Size(max = 8, message = "닉네임은 최대 8자입니다.")
            @Pattern(regexp = NICKNAME_PATTERN, message = "닉네임은 한글/영문/숫자만 가능합니다.")
            String nickname,

            @Parameter(description = "프로필 이미지 (jpg/png/webp, 5MB 이하, 선택사항)")
            @RequestParam(required = false)
            MultipartFile profileImage) {

        Long userId = principal.id();
        String normalizedNickname = userService.normalizeNickname(nickname);

        log.info(" 프로필 업데이트 요청 - 사용자 ID: {}, 닉네임: {}", userId, normalizedNickname);

        userService.updateUserProfile(userId, normalizedNickname, profileImage);

        log.info(" 프로필 업데이트 완료 - 사용자 ID: {}, 닉네임: {}", userId, normalizedNickname);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 프로필 이미지만 업데이트 API - 인증 필요
     *
     * 닉네임 변경 없이 프로필 이미지만 변경할 때 사용합니다.
     *
     * 요청 형식: multipart/form-data
     * - profileImage: 프로필 이미지 파일 (필수, jpg/png/webp, 5MB 이하)
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param profileImage 프로필 이미지 파일
     * @return 업데이트 결과
     */
    @Operation(
        summary = "프로필 이미지만 업데이트",
        description = "프로필 이미지만 변경합니다."
    )
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMyProfileImage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal,

            @Parameter(description = "프로필 이미지 (jpg/png/webp, 5MB 이하)")
            @RequestParam(required = false)
            MultipartFile profileImage) {

        Long userId = principal.id();

        log.info(" 프로필 이미지 업데이트 요청 - 사용자 ID: {}", userId);

        userService.updateProfileImage(userId, profileImage);

        log.info(" 프로필 이미지 업데이트 완료 - 사용자 ID: {}", userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 회원 탈퇴 API - 인증 필요
     *
     * @deprecated 대신 POST /api/me/withdraw 사용을 권장합니다.
     *
     * 사용자 계정을 탈퇴 처리하고 withdrawn_at에 탈퇴 일시를 기록합니다.
     * 동시에 탈퇴 사유를 withdrawal_reason 테이블에 저장합니다.
     *
     * 요청 본문:
     * {
     *   "reasonCode": "RARELY_USED|NO_NEARBY_POSTS|INCONVENIENT|PRIVACY_CONCERN|BAD_EXPERIENCE|OTHER",
     *   "reasonDetail": "선택적 상세 사유 (최대 100자)",
     *   "agreedToTerms": true
     * }
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @param request 탈퇴 요청 정보 (사유 코드 및 상세)
     * @return 탈퇴 응답 정보
     */
    @Deprecated
    @Operation(
        summary = "회원 탈퇴 (Deprecated)",
        description = " Deprecated: POST /api/me/withdraw 사용을 권장합니다. " +
                "사용자 계정을 탈퇴 처리하고 withdrawn_at에 탈퇴 일시를 기록합니다. 탈퇴 사유는 withdrawal_reason 테이블에 저장됩니다.",
        deprecated = true
    )
    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody @Valid WithdrawRequest request) {

        Long userId = principal.id();
        log.warn(" Deprecated API 사용 - /users/me/withdraw → /api/me/withdraw 권장");
        log.info(" 회원 탈퇴 요청 - 사용자 ID: {}, 사유: {}", userId, request.getReasonCode());

        userService.withdrawUser(userId, request);

        log.info(" 회원 탈퇴 완료 - 사용자 ID: {}", userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * 회원 탈퇴 사유 조회 API - 인증 필요
     *
     * 탈퇴한 사용자의 탈퇴 사유 및 상세 정보를 조회합니다.
     * 관리자 또는 해당 사용자만 조회 가능합니다.
     *
     * @param principal JWT에서 추출된 사용자 정보
     * @return 탈퇴 사유 정보
     */
    @Operation(
        summary = "회원 탈퇴 사유 조회",
        description = "탈퇴한 사용자의 탈퇴 사유 세부 내용을 조회합니다."
    )
    @GetMapping("/me/withdraw/reasons")
    public ResponseEntity<WithdrawalReasonResponse> getWithdrawalReason(
            @Parameter(hidden = true)
            @AuthenticationPrincipal JwtUserPrincipal principal) {

        try {
            Long userId = principal.id();
            log.info(" 회원 탈퇴 사유 조회 요청 - 사용자 ID: {}", userId);

            WithdrawalReasonResponse response = userService.getWithdrawalReason(userId);

            log.info(" 회원 탈퇴 사유 조회 완료 - 사용자 ID: {}, 사유: {}", userId, response.getReasonCode());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(" 회원 탈퇴 사유 조회 중 오류 발생 - 사용자 ID: {}", principal.id(), e);
            throw e;
        }
    }
}
