package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.MannerReviewService;
import com.sobunsobun.backend.support.response.ApiResponse;
import com.sobunsobun.backend.dto.manner.MannerReviewRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 매너 평가 컨트롤러
 */
@Slf4j
@Tag(name = "User - 매너 평가", description = "매너 태그 평가 API")
@RestController
@RequestMapping("/api/manner")
@RequiredArgsConstructor
public class MannerReviewController {

    private final MannerReviewService mannerReviewService;

    /**
     * 매너 평가 제출 (여러 명 일괄)
     *
     * POST /api/manner/review
     * Body: {
     *   "groupPostId": 1,
     *   "reviews": [
     *     { "receiverId": 2, "tagCodes": ["TAG001", "TAG002"] },
     *     { "receiverId": 3, "tagCodes": ["TAG003"] }
     *   ]
     * }
     * Response: { 2: ["TAG001", "TAG002"], 3: ["TAG003"] }
     */
    @Operation(
        summary = "매너 평가 제출 (일괄)",
        description = "거래 완료 후 여러 참여자에 대한 매너 태그를 한 번에 평가합니다. " +
                      "동일 거래에서 동일 대상에 대한 중복 평가는 무시됩니다. " +
                      "응답은 receiverId를 키로, 실제 저장된 태그 목록을 값으로 반환합니다."
    )
    @PostMapping("/review")
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @Valid @RequestBody MannerReviewRequest request,
            Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        log.info("매너 평가 요청 - senderId: {}", principal.id());

        mannerReviewService.submitMannerReviews(request, principal.id());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
