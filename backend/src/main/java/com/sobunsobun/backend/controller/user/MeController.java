package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.dto.user.LocationVerificationRequest;
import com.sobunsobun.backend.dto.user.LocationVerificationResponse;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자 기본 정보 조회 컨트롤러
 *
 * 담당 기능:
 * - 간단한 사용자 정보 조회 (/me)
 * - 위치 인증 정보 조회 및 업데이트
 * - 사용자 기본 데이터 제공
 *
 * 참고:
 * - 프로필 관련 상세 기능은 MyProfileController 사용
 * - 설정 관련 기능은 MySettingsController 사용
 * - 계정 관리는 MyAccountController 사용
 */
@Slf4j
@Tag(name = "User - 내 정보", description = "내 기본 정보 및 위치 인증 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository users;
    private final UserService userService;

    /**
     * 내 정보 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 사용자 요약 정보
     */
    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 기본 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummary> me(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("👤 사용자 정보 조회 요청 - 사용자 ID: {}", principal.id());

            User user = users.findById(principal.id())
                    .orElseThrow(() -> {
                        log.warn("⚠️ 사용자를 찾을 수 없음 - 사용자 ID: {}", principal.id());
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다");
                    });

            log.info("✅ 사용자 정보 조회 완료 - 사용자 ID: {}, 닉네임: {}", principal.id(), user.getNickname());

            return ResponseEntity.ok(
                    AuthResponse.UserSummary.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .profileImageUrl(user.getProfileImageUrl())
                            .address(user.getAddress())
                            .role(user.getRole())
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ 사용자 정보 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 위치 인증 정보 조회
     *
     * 현재 사용자의 위치 인증 상태를 확인합니다.
     * - 위치 인증 여부
     * - 인증된 주소
     * - 인증 만료 여부 (24시간 기준)
     * - 인증 만료까지 남은 시간 (분 단위)
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 위치 인증 정보
     */
    @Operation(
        summary = "위치 인증 정보 조회",
        description = "현재 사용자의 위치 인증 상태를 조회합니다. 위치 인증은 24시간 동안 유효합니다."
    )
    @GetMapping("/me/location-verification")
    public ResponseEntity<LocationVerificationResponse> getLocationVerification(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📍 위치 인증 정보 조회 요청 - 사용자 ID: {}", principal.id());

            LocationVerificationResponse response = userService.getLocationVerification(principal.id());

            log.info("✅ 위치 인증 정보 조회 완료 - 사용자 ID: {}, 인증여부: {}",
                    principal.id(), response.isVerified());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 위치 인증 정보 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 위치 인증 업데이트
     *
     * 사용자의 위치를 인증하고 주소 정보를 업데이트합니다.
     * 위치 인증은 24시간 동안 유효하며, 이후에는 재인증이 필요합니다.
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 위치 인증 요청 (주소만 포함)
     * @return 업데이트된 위치 인증 정보
     */
    @Operation(
        summary = "위치 인증 업데이트",
        description = "사용자의 위치를 인증합니다. 인증된 위치는 24시간 동안 유효합니다."
    )
    @PatchMapping("/me/location-verification")
    public ResponseEntity<LocationVerificationResponse> updateLocationVerification(
            Authentication authentication,
            @Valid @RequestBody LocationVerificationRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            String address = request.getAddress();

            log.info("📍 위치 인증 업데이트 요청 - 사용자 ID: {}, 주소: {}", principal.id(), address);

            // 위치 인증 업데이트
            userService.updateLocationVerification(principal.id(), address);

            // 업데이트된 정보 조회 후 반환
            LocationVerificationResponse response = userService.getLocationVerification(principal.id());

            log.info("✅ 위치 인증 업데이트 완료 - 사용자 ID: {}, 주소: {}", principal.id(), address);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 위치 인증 업데이트 중 오류 발생", e);
            throw e;
        }
    }
}

