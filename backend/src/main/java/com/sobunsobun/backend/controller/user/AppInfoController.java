package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.settings.AppInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 앱 정보 컨트롤러
 *
 * 담당 기능:
 * - 최신 앱 버전 정보 조회
 *
 * 특징:
 * - 인증 불필요한 공개 API
 * - 모든 사용자가 접근 가능
 */
@Slf4j
@Tag(name = "User - 앱 정보", description = "앱 버전 정보 API")
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppInfoController {

    @Operation(summary = "최신 앱 버전 정보 조회")
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<AppInfoResponse>> getAppVersion() {
        try {
            log.info(" 앱 버전 정보 조회 요청");

            AppInfoResponse appInfo = AppInfoResponse.builder()
                    .latestVersion("1.0.0")
                    .updateRequired(false)
                    .releaseNotes("새로운 버전이 출시되었습니다.")
                    .build();

            log.info(" 앱 버전 정보 조회 완료 - 버전: {}", appInfo.getLatestVersion());

            return ResponseEntity.ok(ApiResponse.success(appInfo));
        } catch (Exception e) {
            log.error(" 앱 버전 정보 조회 중 오류 발생", e);
            throw e;
        }
    }
}

