package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.SettingsService;
import com.sobunsobun.backend.dto.settings.NotificationSettingRequest;
import com.sobunsobun.backend.dto.settings.NotificationSettingResponse;
import com.sobunsobun.backend.dto.settings.RegionSettingRequest;
import com.sobunsobun.backend.dto.settings.RegionSettingResponse;
import com.sobunsobun.backend.dto.settings.SettingsResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "User - 설정", description = "지역 및 알림 설정 API")
@RestController
@RequestMapping("/api/me/settings")
@RequiredArgsConstructor
public class MySettingsController {

    private final SettingsService settingsService;

    @Operation(
        summary = "전체 설정 조회",
        description = "사용자의 알림 설정을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SettingsResponse>> getSettings(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        NotificationSettingResponse notification = settingsService.getNotificationSetting(principal.id());
        SettingsResponse settings = SettingsResponse.builder()
                .notification(notification)
                .build();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @Operation(
        summary = "내 지역 설정 조회",
        description = "사용자가 설정한 지역 정보를 조회합니다."
    )
    @GetMapping("/region")
    public ResponseEntity<ApiResponse<RegionSettingResponse>> getRegionSetting(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        // TODO: 지역 설정 서비스 구현 필요
        return ResponseEntity.ok(ApiResponse.success(RegionSettingResponse.builder().build()));
    }

    @Operation(
        summary = "내 지역 설정 변경",
        description = "사용자의 지역 설정을 변경합니다."
    )
    @PatchMapping("/region")
    public ResponseEntity<ApiResponse<RegionSettingResponse>> updateRegionSetting(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody RegionSettingRequest request) {
        // TODO: 지역 설정 서비스 구현 필요
        return ResponseEntity.ok(ApiResponse.success(RegionSettingResponse.builder().build()));
    }

    @Operation(
        summary = "알림 설정 조회",
        description = "사용자의 알림 수신 설정을 조회합니다."
    )
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getNotificationSetting(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        NotificationSettingResponse response = settingsService.getNotificationSetting(principal.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "알림 설정 변경",
        description = "사용자의 알림 수신 설정을 변경합니다."
    )
    @PatchMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateNotificationSetting(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody NotificationSettingRequest request) {
        NotificationSettingResponse response = settingsService.updateNotificationSetting(principal.id(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
