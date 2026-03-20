package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.ProfileService;
import com.sobunsobun.backend.application.user.ReportService;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.dto.profile.MyProfileDetailResponse;
import com.sobunsobun.backend.dto.profile.PublicUserProfileResponse;
import com.sobunsobun.backend.dto.user.UserReportRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 유저 프로필 및 신고 API 컨트롤러
 *
 * 엔드포인트:
 * - GET  /api/v1/users/me/profile           : 내 프로필 조회 (탭별 게시글)
 * - GET  /api/v1/users/{userId}/profile     : 타 유저 프로필 조회
 * - POST /api/v1/users/{userId}/report      : 유저 신고
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User - 프로필 v1", description = "프로필 조회 및 유저 신고 API")
public class UserProfileController {

    private final ProfileService profileService;
    private final ReportService reportService;

    /**
     * 내 프로필 조회 (탭별 게시글 목록 페이징)
     *
     * @param principal 인증된 사용자
     * @param tab       조회 탭: posts(내 글, 기본값) | commented(댓글 단 글) | saved(저장한 글)
     * @param page      페이지 번호 (0부터 시작)
     * @param size      페이지 크기
     */
    @GetMapping("/me/profile")
    @Operation(
            summary = "내 프로필 조회",
            description = "내 프로필 정보와 탭별 게시글 목록을 조회합니다.\n\n" +
                          "- `tab=posts` : 내가 작성한 글\n" +
                          "- `tab=commented` : 내가 댓글을 단 글\n" +
                          "- `tab=saved` : 내가 저장한 글",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<MyProfileDetailResponse>> getMyProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "조회 탭 (posts | commented | saved)", example = "posts")
            @RequestParam(defaultValue = "posts") String tab,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("내 프로필 조회 요청 - userId: {}, tab: {}, page: {}, size: {}",
                principal.id(), tab, page, size);

        MyProfileDetailResponse response = profileService.getMyProfile(principal.id(), tab, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 타 유저 프로필 조회 (작성 게시글 목록 페이징)
     *
     * @param userId    조회할 사용자 ID
     * @param principal 현재 인증된 사용자 (선택사항)
     * @param page      페이지 번호
     * @param size      페이지 크기
     */
    @GetMapping("/{userId}/profile")
    @Operation(
            summary = "타 유저 프로필 조회",
            description = "다른 유저의 프로필 정보와 작성한 게시글 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<PublicUserProfileResponse>> getUserProfile(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = principal != null ? principal.id() : null;
        log.info("타 유저 프로필 조회 요청 - currentUserId: {}, targetUserId: {}, page: {}, size: {}", 
                currentUserId, userId, page, size);

        PublicUserProfileResponse response = profileService.getUserProfile(currentUserId, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 유저 신고
     *
     * @param userId    신고 대상 사용자 ID
     * @param principal 신고자 (인증된 사용자)
     * @param request   신고 사유 및 내용
     */
    @PostMapping("/{userId}/report")
    @Operation(
            summary = "유저 신고",
            description = "다른 유저를 신고합니다. 자기 자신은 신고할 수 없습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.sobunsobun.backend.support.response.ApiResponse<Void>> reportUser(
            @Parameter(description = "신고 대상 사용자 ID", required = true)
            @PathVariable Long userId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UserReportRequest request
    ) {
        log.info("유저 신고 요청 - reporterId: {}, targetUserId: {}, reason: {}",
                principal.id(), userId, request.getReason());

        reportService.reportUser(principal.id(), userId, request);
        return ResponseEntity.ok(com.sobunsobun.backend.support.response.ApiResponse.ok());
    }
}
