package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.device.*;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 디바이스 관리 (FCM 토큰) API 컨트롤러
 *
 * 담당 기능:
 * - FCM 토큰 등록/갱신
 * - FCM 토큰 삭제
 * - FCM 토큰 활성화/비활성화
 *
 * TODO: DeviceService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 디바이스", description = "FCM 디바이스 관리 API")
@RestController
@RequestMapping("/api/me/devices")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeviceController {

    // TODO: DeviceService 주입 및 구현
    // private final DeviceService deviceService;

    @Operation(
        summary = "FCM 토큰 등록/갱신",
        description = "디바이스의 FCM 토큰을 등록하거나 갱신합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceRegistrationResponse>> registerDevice(
            Authentication authentication,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📱 FCM 토큰 등록 요청 - 사용자 ID: {}, 디바이스: {}", principal.id(), request);

            // TODO: 서비스 로직 구현
            // DeviceRegistrationResponse response = deviceService.registerDevice(principal.id(), request);

            // 임시 응답
            DeviceRegistrationResponse response = DeviceRegistrationResponse.builder()
                    .build();

            log.info("✅ FCM 토큰 등록 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ FCM 토큰 등록 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(
        summary = "FCM 토큰 삭제",
        description = "로그아웃 시 디바이스의 FCM 토큰을 삭제합니다."
    )
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceDeleteResponse>> deleteDevice(
            Authentication authentication,
            @PathVariable String deviceId) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📱 FCM 토큰 삭제 요청 - 사용자 ID: {}, 디바이스 ID: {}", principal.id(), deviceId);

            // TODO: 서비스 로직 구현
            // deviceService.deleteDevice(principal.id(), deviceId);

            // 임시 응답
            DeviceDeleteResponse response = DeviceDeleteResponse.builder()
                    .build();

            log.info("✅ FCM 토큰 삭제 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ FCM 토큰 삭제 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(
        summary = "FCM 토큰 활성화/비활성화",
        description = "특정 디바이스의 푸시 알림을 활성화하거나 비활성화합니다."
    )
    @PatchMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceUpdateResponse>> updateDevice(
            Authentication authentication,
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("📱 FCM 토큰 상태 변경 요청 - 사용자 ID: {}, 디바이스 ID: {}, 상태: {}",
                    principal.id(), deviceId, request);

            // TODO: 서비스 로직 구현
            // deviceService.updateDevice(principal.id(), deviceId, request);

            // 임시 응답
            DeviceUpdateResponse response = DeviceUpdateResponse.builder()
                    .build();

            log.info("✅ FCM 토큰 상태 변경 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ FCM 토큰 상태 변경 중 오류 발생", e);
            throw e;
        }
    }
}
