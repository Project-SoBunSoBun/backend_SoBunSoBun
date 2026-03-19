package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.MyProfileService;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.mypage.MyProfileResponse;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateRequestDto;
import com.sobunsobun.backend.dto.mypage.ProfileUpdateResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 관리 컨트롤러
 *
 * 담당 기능:
 * - 프로필 조회 (닉네임, 프로필 이미지, 매너 점수, 통계, 매너 태그)
 * - 프로필 수정 (닉네임, 프로필 이미지)
 *
 * 이 컨트롤러는 마이페이지 프로필에 대한 상세 기능을 제공합니다.
 * 기본 프로필 수정은 UserController를 참고하세요.
 */
@Slf4j
@Tag(name = "User - 프로필", description = "프로필 상세 조회 및 수정 API")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyProfileController {

    private final MyProfileService myProfileService;

    /**
     * 마이페이지 프로필 조회
     *
     * 조회 항목:
     * - 기본 정보: userId, nickname, profileImageUrl
     * - 활동 통계: mannerScore, participationCount, hostCount
     * - 매너 태그: 받은 매너 평가 태그 목록 (상위 5개)
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 프로필 정보
     */
    @Operation(
        summary = "마이페이지 프로필 조회",
        description = "사용자의 프로필 정보, 활동 통계, 매너 태그를 조회합니다."
    )
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getProfile(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info(" 프로필 조회 요청 - 사용자 ID: {}", principal.id());

            MyProfileResponse profile = myProfileService.getProfile(principal.id());

            log.info(" 프로필 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(profile));
        } catch (Exception e) {
            log.error(" 프로필 조회 중 오류 발생", e);
            throw e;
        }
    }

//    /**
//     * 프로필 수정
//     *
//     * 수정 가능 항목:
//     * - nickname: 2~20자, 한글/영문/숫자, 중복 불가
//     * - profileImageUrl: 이미지 URL (최대 500자)
//     *
//     * @param authentication 현재 로그인한 사용자 인증 정보
//     * @param request 프로필 수정 요청
//     * @return 수정된 프로필 정보
//     */
//    @Operation(
//        summary = "프로필 수정",
//        description = "닉네임 또는 프로필 이미지를 수정합니다."
//    )
//    @PatchMapping("/profile")
//    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
//            Authentication authentication,
//            @Valid @RequestBody ProfileUpdateRequestDto request) {
//        try {
//            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
//            log.info(" 프로필 수정 요청 - 사용자 ID: {}, 닉네임: {}", principal.id(), request.getNickname());
//
//            ProfileUpdateResponse response = myProfileService.updateProfile(principal.id(), request);
//
//            log.info(" 프로필 수정 완료 - 사용자 ID: {}", principal.id());
//
//            return ResponseEntity.ok(ApiResponse.success(response));
//        } catch (Exception e) {
//            log.error(" 프로필 수정 중 오류 발생", e);
//            throw e;
//        }
//    }
}

