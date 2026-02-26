package com.sobunsobun.backend.controller.user;

import com.sobunsobun.backend.application.user.MannerReviewService;
import com.sobunsobun.backend.dto.common.ApiResponse;
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
     * 매너 평가 제출
     *
     * POST /api/manner/review
     * Body: { receiverId, groupPostId, tagCodes: ["TAG001", "TAG002"] }
     */
    @Operation(
        summary = "매너 평가 제출",
        description = "거래 완료 후 상대방에 대한 매너 태그를 선택하여 평가합니다. 동일 거래에서 중복 평가는 무시됩니다."
    )
    @PostMapping("/review")
    public ResponseEntity<ApiResponse<List<String>>> submitReview(
            @Valid @RequestBody MannerReviewRequest request,
            Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        log.info("매너 평가 요청 - senderId: {}", principal.id());

        List<String> savedTags = mannerReviewService.submitMannerReview(request, principal.id());
        return ResponseEntity.ok(ApiResponse.success(savedTags));
    }
}