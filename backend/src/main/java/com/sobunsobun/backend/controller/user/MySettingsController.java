package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.settings.*;
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
 * 사용자 설정 관리 컨트롤러
 *
 * 담당 기능:
 * - 전체 설정 조회
 * - 지역 설정 조회/변경
 * - 알림 설정 조회/변경
 *
 * TODO: SettingsService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 설정", description = "지역 및 알림 설정 API")
@RestController
@RequestMapping("/api/me/settings")
@RequiredArgsConstructor
public class MySettingsController {

    // TODO: SettingsService 주입 및 구현
    // private final SettingsService settingsService;

    /**
     * 전체 설정 조회
     *
     * 조회 항목:
     * - 지역 설정
     * - 알림 설정 (모든 항목)
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 전체 설정 정보
     */
    @Operation(
        summary = "전체 설정 조회",
        description = "사용자의 지역 설정과 알림 설정을 모두 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("⚙️ 전체 설정 조회 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // SettingsResponse settings = settingsService.getSettings(principal.id());

            // 임시 응답
            SettingsResponse settings = SettingsResponse.builder()
                    .build();

            log.info("✅ 전체 설정 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            log.error("❌ 전체 설정 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 내 지역 설정 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 지역 설정 정보
     */
    @Operation(
        summary = "내 지역 설정 조회",
        description = "사용자가 설정한 지역 정보를 조회합니다."
    )
    @GetMapping("/region")
    public ResponseEntity<ApiResponse<RegionSettingResponse>> getRegionSetting(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🗺️ 지역 설정 조회 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // RegionSettingResponse region = settingsService.getRegionSetting(principal.id());

            // 임시 응답
            RegionSettingResponse region = RegionSettingResponse.builder()
                    .build();

            log.info("✅ 지역 설정 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(region));
        } catch (Exception e) {
            log.error("❌ 지역 설정 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 내 지역 설정 변경
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 지역 설정 변경 요청
     * @return 변경된 지역 설정 정보
     */
    @Operation(
        summary = "내 지역 설정 변경",
        description = "사용자의 지역 설정을 변경합니다."
    )
    @PatchMapping("/region")
    public ResponseEntity<ApiResponse<RegionSettingResponse>> updateRegionSetting(
            Authentication authentication,
            @Valid @RequestBody RegionSettingRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🗺️ 지역 설정 변경 요청 - 사용자 ID: {}, 새로운 지역: {}", principal.id(), request);

            // TODO: Service 호출로 교체
            // RegionSettingResponse region = settingsService.updateRegionSetting(principal.id(), request);

            // 임시 응답
            RegionSettingResponse region = RegionSettingResponse.builder()
                    .build();

            log.info("✅ 지역 설정 변경 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(region));
        } catch (Exception e) {
            log.error("❌ 지역 설정 변경 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 알림 설정 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 알림 설정 정보
     */
    @Operation(
        summary = "알림 설정 조회",
        description = "사용자의 알림 수신 설정을 조회합니다."
    )
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getNotificationSetting(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 알림 설정 조회 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // NotificationSettingResponse notification = settingsService.getNotificationSetting(principal.id());

            // 임시 응답
            NotificationSettingResponse notification = NotificationSettingResponse.builder()
                    .build();

            log.info("✅ 알림 설정 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(notification));
        } catch (Exception e) {
            log.error("❌ 알림 설정 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 알림 설정 변경
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param request 알림 설정 변경 요청
     * @return 변경된 알림 설정 정보
     */
    @Operation(
        summary = "알림 설정 변경",
        description = "사용자의 알림 수신 설정을 변경합니다."
    )
    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateNotificationSetting(
            Authentication authentication,
            @Valid @RequestBody NotificationSettingRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 알림 설정 변경 요청 - 사용자 ID: {}, 새로운 설정: {}", principal.id(), request);

            // TODO: Service 호출로 교체
            // NotificationSettingResponse notification = settingsService.updateNotificationSetting(principal.id(), request);

            // 임시 응답
            NotificationSettingResponse notification = NotificationSettingResponse.builder()
                    .build();

            log.info("✅ 알림 설정 변경 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(notification));
        } catch (Exception e) {
            log.error("❌ 알림 설정 변경 중 오류 발생", e);
            throw e;
        }
    }
}

