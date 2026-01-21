package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.common.PageResponse;
import com.sobunsobun.backend.dto.notification.NotificationItemResponse;
import com.sobunsobun.backend.dto.notification.UnreadCountResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 알림 내역 관리 컨트롤러
 *
 * 담당 기능:
 * - 알림 내역 목록 조회
 * - 알림 읽음 처리
 * - 전체 알림 읽음 처리
 * - 읽지 않은 알림 수 조회
 *
 * TODO: NotificationService 주입 및 구현
 */
@Slf4j
@Tag(name = "User - 알림", description = "알림 내역 관리 API")
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    // TODO: NotificationService 주입 및 구현
    // private final NotificationService notificationService;

    /**
     * 알림 내역 목록 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param pageable 페이지네이션 정보 (기본: 0페이지, 20개, 최신순)
     * @return 알림 내역 목록
     */
    @Operation(
        summary = "알림 내역 목록 조회",
        description = "사용자의 알림 내역을 페이지네이션하여 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationItemResponse>>> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 알림 목록 조회 요청 - 사용자 ID: {}, 페이지: {}", principal.id(), pageable.getPageNumber());

            // TODO: Service 호출로 교체
            // PageResponse<NotificationItemResponse> notifications = notificationService.getNotifications(principal.id(), pageable);

            // 임시 응답
            PageResponse<NotificationItemResponse> notifications = new PageResponse<>();

            log.info("✅ 알림 목록 조회 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            log.error("❌ 알림 목록 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 읽지 않은 알림 수 조회
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 읽지 않은 알림 수
     */
    @Operation(
        summary = "읽지 않은 알림 수 조회",
        description = "사용자의 읽지 않은 알림 개수를 조회합니다."
    )
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 읽지 않은 알림 수 조회 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // UnreadCountResponse count = notificationService.getUnreadCount(principal.id());

            // 임시 응답
            UnreadCountResponse count = UnreadCountResponse.builder()
                    .unreadCount(0)
                    .build();

            log.info("✅ 읽지 않은 알림 수 조회 완료 - 사용자 ID: {}, 개수: {}", principal.id(), count.getUnreadCount());

            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("❌ 읽지 않은 알림 수 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 알림 읽음 처리
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @param id 알림 ID
     * @return 성공 메시지
     */
    @Operation(
        summary = "알림 읽음 처리",
        description = "특정 알림을 읽음 처리합니다."
    )
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            Authentication authentication,
            @PathVariable @Parameter(description = "알림 ID") Long id) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 알림 읽음 처리 요청 - 사용자 ID: {}, 알림 ID: {}", principal.id(), id);

            // TODO: Service 호출로 교체
            // notificationService.markAsRead(principal.id(), id);

            log.info("✅ 알림 읽음 처리 완료 - 사용자 ID: {}, 알림 ID: {}", principal.id(), id);

            return ResponseEntity.ok(ApiResponse.success("알림이 읽음 처리되었습니다."));
        } catch (Exception e) {
            log.error("❌ 알림 읽음 처리 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 전체 알림 읽음 처리
     *
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 성공 메시지
     */
    @Operation(
        summary = "전체 알림 읽음 처리",
        description = "사용자의 모든 알림을 읽음 처리합니다."
    )
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication) {
        try {
            JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
            log.info("🔔 전체 알림 읽음 처리 요청 - 사용자 ID: {}", principal.id());

            // TODO: Service 호출로 교체
            // notificationService.markAllAsRead(principal.id());

            log.info("✅ 전체 알림 읽음 처리 완료 - 사용자 ID: {}", principal.id());

            return ResponseEntity.ok(ApiResponse.success("모든 알림이 읽음 처리되었습니다."));
        } catch (Exception e) {
            log.error("❌ 전체 알림 읽음 처리 중 오류 발생", e);
            throw e;
        }
    }
}


