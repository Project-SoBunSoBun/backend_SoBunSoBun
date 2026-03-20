package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.notification.NotificationService;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.dto.notification.NotificationItemResponse;
import com.sobunsobun.backend.dto.notification.UnreadCountResponse;

import java.util.Map;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User - 알림", description = "알림 목록 및 읽음 처리 API")
@RestController
@RequestMapping("/api/me/notifications")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationItemResponse>>> getNotifications(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<NotificationItemResponse> response =
                notificationService.getNotifications(principal.id(), page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "읽지 않은 알림 수 조회", description = "읽지 않은 알림 개수를 반환합니다.")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        UnreadCountResponse response = notificationService.getUnreadCount(principal.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "단건 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> readNotification(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long id) {
        notificationService.readNotification(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "전체 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readAll(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        notificationService.readAll(principal.id());
        return ResponseEntity.ok(ApiResponse.success(Map.of()));
    }
}
