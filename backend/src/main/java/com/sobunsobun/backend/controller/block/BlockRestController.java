package com.sobunsobun.backend.controller.block;

import com.sobunsobun.backend.application.block.BlockService;
import com.sobunsobun.backend.dto.block.BlockedUserResponse;
import com.sobunsobun.backend.dto.common.ApiResponse;
import com.sobunsobun.backend.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Tag(name = "Block - 차단", description = "사용자 차단 관리 API")
public class BlockRestController {

    private final BlockService blockService;

    /**
     * POST /api/v1/blocks/{userId}
     * 특정 사용자를 차단합니다.
     */
    @PostMapping("/{userId}")
    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다. 자기 자신이나 이미 차단된 사용자는 불가.")
    public ResponseEntity<com.sobunsobun.backend.support.response.ApiResponse<Void>> blockUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        blockService.blockUser(currentUserId, userId);
        return ResponseEntity.ok(com.sobunsobun.backend.support.response.ApiResponse.ok());
    }

    /**
     * DELETE /api/v1/blocks/{userId}
     * 특정 사용자의 차단을 해제합니다.
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "차단 취소", description = "차단된 사용자를 차단 해제합니다.")
    public ResponseEntity<com.sobunsobun.backend.support.response.ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        blockService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(com.sobunsobun.backend.support.response.ApiResponse.ok());
    }

    /**
     * GET /api/v1/blocks
     * 내가 차단한 사용자 목록을 조회합니다.
     */
    @GetMapping
    @Operation(summary = "차단 목록 조회", description = "내가 차단한 사용자 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<BlockedUserResponse>>> getBlockedUsers() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        List<BlockedUserResponse> result = blockService.getBlockedUsers(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
