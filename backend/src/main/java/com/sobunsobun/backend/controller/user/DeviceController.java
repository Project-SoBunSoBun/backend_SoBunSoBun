package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.DeviceService;
import com.sobunsobun.backend.dto.device.DeviceDeleteResponse;
import com.sobunsobun.backend.dto.device.DeviceRegistrationRequest;
import com.sobunsobun.backend.dto.device.DeviceRegistrationResponse;
import com.sobunsobun.backend.dto.device.DeviceUpdateRequest;
import com.sobunsobun.backend.dto.device.DeviceUpdateResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "User - 디바이스", description = "FCM 디바이스 관리 API")
@RestController
@RequestMapping("/api/me/devices")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(
        summary = "FCM 토큰 등록/갱신",
        description = "디바이스의 FCM 토큰을 등록하거나 갱신합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceRegistrationResponse>> registerDevice(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        DeviceRegistrationResponse response = deviceService.registerDevice(principal.id(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "FCM 토큰 삭제",
        description = "로그아웃 시 디바이스의 FCM 토큰을 삭제합니다."
    )
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDeleteResponse>> deleteDevice(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String deviceId) {
        DeviceDeleteResponse response = deviceService.deleteDevice(principal.id(), deviceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "FCM 토큰 활성화/비활성화",
        description = "특정 디바이스의 푸시 알림을 활성화하거나 비활성화합니다."
    )
    @PatchMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceUpdateResponse>> updateDevice(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        DeviceUpdateResponse response = deviceService.updateDevice(principal.id(), deviceId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
