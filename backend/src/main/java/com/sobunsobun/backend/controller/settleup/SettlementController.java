package com.sobunsobun.backend.controller.settleup;

import com.sobunsobun.backend.application.settleup.SettlementService;
import com.sobunsobun.backend.controller.BaseController;
import com.sobunsobun.backend.dto.settleup.SettlementCompleteRequest;
import com.sobunsobun.backend.dto.settleup.SettlementDetailResponse;
import com.sobunsobun.backend.dto.settleup.SettlementSummaryResponse;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import com.sobunsobun.backend.support.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "정산 API", description = "공동구매 정산 관련 API")
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController extends BaseController {

    private final SettlementService settlementService;

    // ──────────────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────────────

    @Operation(summary = "정산 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{settlementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlement(
            @PathVariable Long settlementId) {
        return ok(settlementService.getSettlementDetail(settlementId));
    }

    @Operation(summary = "게시글별 정산 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/post/{groupPostId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlementByPost(
            @PathVariable Long groupPostId) {
        return ok(settlementService.getSettlementByPost(groupPostId));
    }

    @Operation(summary = "내 정산 목록 조회",
               description = "status: ALL(전체), PENDING(미완료), COMPLETED(완료), 미입력 시 전체",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<SettlementSummaryResponse>>> getMySettlements(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "상태 필터 (ALL / PENDING / COMPLETED)")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ok(settlementService.getMySettlements(principal.id(), status, page, size));
    }

    // ──────────────────────────────────────────────────
    // 정산 완료 (iOS → 서버)
    // ──────────────────────────────────────────────────

    @Operation(summary = "정산 완료",
               description = "iOS 클라이언트가 계산한 최종 결과를 서버에 저장합니다. " +
                             "totalAmount와 참여자별 assignedAmount 합계가 일치해야 합니다. " +
                             "이미 완료된 정산도 재제출 가능합니다 (수정).",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{settlementId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SettlementDetailResponse>> completeSettlement(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long settlementId,
            @Valid @RequestBody SettlementCompleteRequest request) {
        return ok(settlementService.completeSettlement(principal.id(), settlementId, request));
    }
}
