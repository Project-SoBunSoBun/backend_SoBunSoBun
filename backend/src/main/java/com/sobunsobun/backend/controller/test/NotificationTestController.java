package com.sobunsobun.backend.controller.test;

import com.sobunsobun.backend.application.notification.NotificationService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.exception.UserException;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 알림 수동 발송 테스트 컨트롤러 (dev/local 환경 전용)
 *
 * POST /api/test/notifications/me       — 나에게 알림 전송
 * POST /api/test/notifications/{userId} — 특정 유저에게 알림 전송 (내가 발신자)
 */
@Tag(name = "[TEST] 알림 테스트", description = "알림 수동 발송 테스트 API (dev/local 환경 전용)")
@RestController
@RequestMapping("/api/test/notifications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Profile({"local", "dev", "default"})
public class NotificationTestController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // DTO
    public record SendRequest(
            @NotBlank String type,
            @NotBlank String title,
            @NotBlank String body
    ) {}

    /**
     * 현재 로그인한 나에게 테스트 알림 전송
     *
     * 사용 가능한 type: COMMENT, CHAT, PARTICIPATION, SETTLEMENT, POST_UPDATE, ANNOUNCEMENT
     */
    @Operation(summary = "나에게 테스트 알림 전송", description = "로그인한 본인에게 DB 저장 + FCM 푸시 알림을 발송합니다.")
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<Void>> sendToMe(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody SendRequest request) {

        User me = userRepository.findById(principal.id())
                .orElseThrow(UserException::notFound);

        notificationService.createAndSend(
                me,
                request.type(),
                request.title(),
                request.body(),
                Map.of("type", request.type())
        );

        return ResponseEntity.ok(ApiResponse.success(null, "알림 전송 완료"));
    }

    /**
     * 특정 userId에게 테스트 알림 전송
     */
    @Operation(summary = "특정 유저에게 테스트 알림 전송", description = "지정한 userId 유저에게 DB 저장 + FCM 푸시 알림을 발송합니다.")
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> sendToUser(
            @PathVariable Long userId,
            @Valid @RequestBody SendRequest request) {

        User target = userRepository.findById(userId)
                .orElseThrow(UserException::notFound);

        notificationService.createAndSend(
                target,
                request.type(),
                request.title(),
                request.body(),
                Map.of("type", request.type())
        );

        return ResponseEntity.ok(ApiResponse.success(null, "알림 전송 완료 (userId=" + userId + ")"));
    }
}
