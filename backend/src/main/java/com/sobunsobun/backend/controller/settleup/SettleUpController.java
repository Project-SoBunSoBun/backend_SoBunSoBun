package com.sobunsobun.backend.controller.settleup;

import com.sobunsobun.backend.application.settleup.SettleUpService;
import com.sobunsobun.backend.dto.settleup.SettleUpCreateRequest;
import com.sobunsobun.backend.dto.settleup.SettleUpResponse;
import com.sobunsobun.backend.dto.settleup.SettleUpUpdateRequest;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정산 API 컨트롤러
 *
 * 엔드포인트:
 * - POST   /api/settleups                     : 정산 생성 (인증 필요)
 * - GET    /api/settleups                     : 전체 정산 목록 조회 (공개)
 * - GET    /api/settleups/{id}                : 정산 단건 조회 (공개)
 * - PUT    /api/settleups/{id}                : 정산 수정 (인증 필요, 생성자만)
 * - DELETE /api/settleups/{id}                : 정산 삭제 (인증 필요, 생성자만)
 * - PATCH  /api/settleups/{id}/status         : 정산 상태 변경 (인증 필요, 생성자만)
 * - GET    /api/settleups/group-post/{id}     : 공동구매 게시글별 정산 목록 조회 (공개)
 * - GET    /api/settleups/my                  : 내 정산 목록 조회 (인증 필요)
 */
@Slf4j
@RestController
@RequestMapping("/api/settleups")
@RequiredArgsConstructor
@Tag(name = "정산", description = "정산 API")
public class SettleUpController {

    private final SettleUpService settleUpService;

    /**
     * 정산 생성
     *
     * @param principal 인증된 사용자 정보
     * @param request 정산 생성 요청 데이터
     * @return 201 Created, 생성된 정산 정보
     */
    @PostMapping
    @Operation(
        summary = "정산 생성 (개발중)",
        description = "새로운 정산을 생성합니다. (인증 필요)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SettleUpResponse> createSettleUp(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody SettleUpCreateRequest request
    ) {
        log.info("[API 호출] POST /api/settleups - 사용자 ID: {}", principal.id());
        SettleUpResponse response = settleUpService.createSettleUp(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 정산 단건 조회
     *
     * @param id 정산 ID
     * @return 200 OK, 정산 정보
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "정산 단건 조회 (개발중)",
        description = "정산 ID로 정산 정보를 조회합니다."
    )
    public ResponseEntity<SettleUpResponse> getSettleUp(
            @Parameter(description = "정산 ID", example = "1")
            @PathVariable Long id
    ) {
        log.info("[API 호출] GET /api/settleups/{} - 정산 조회", id);
        SettleUpResponse response = settleUpService.getSettleUp(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 정산 수정
     *
     * @param principal 인증된 사용자 정보
     * @param id 정산 ID
     * @param request 정산 수정 요청 데이터
     * @return 200 OK, 수정된 정산 정보
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "정산 수정 (개발중)",
        description = "정산 정보를 수정합니다. (인증 필요, 생성자만 가능)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SettleUpResponse> updateSettleUp(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "정산 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody SettleUpUpdateRequest request
    ) {
        log.info("[API 호출] PUT /api/settleups/{} - 사용자 ID: {}", id, principal.id());
        SettleUpResponse response = settleUpService.updateSettleUp(principal.id(), id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 정산 삭제
     *
     * @param principal 인증된 사용자 정보
     * @param id 정산 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "정산 삭제",
        description = "정산을 삭제합니다. (인증 필요, 생성자만 가능)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deleteSettleUp(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "정산 ID", example = "1")
            @PathVariable Long id
    ) {
        log.info("[API 호출] DELETE /api/settleups/{} - 사용자 ID: {}", id, principal.id());
        settleUpService.deleteSettleUp(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 정산 상태 변경
     *
     * @param principal 인증된 사용자 정보
     * @param id 정산 ID
     * @param status 변경할 상태
     * @return 200 OK, 변경된 정산 정보
     */
    @PatchMapping("/{id}/status")
    @Operation(
        summary = "정산 상태 변경",
        description = "정산 상태를 변경합니다. (인증 필요, 생성자만 가능)\n상태값: 1=미정산, 2=정산완료, 3=삭제됨",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SettleUpResponse> updateSettleUpStatus(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "정산 ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "정산 상태 (1: 미정산, 2: 정산완료, 3: 삭제됨)", example = "1")
            @RequestParam Integer status
    ) {
        log.info("[API 호출] PATCH /api/settleups/{}/status - 사용자 ID: {}, 상태: {}", id, principal.id(), status);
        SettleUpResponse response = settleUpService.updateSettleUpStatus(principal.id(), id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * 공동구매 게시글별 정산 목록 조회
     *
     * @param groupPostId 공동구매 게시글 ID
     * @param activeOnly 활성 정산만 조회 여부 (기본값: false)
     * @return 200 OK, 정산 목록
     */
    @GetMapping("/group-post/{groupPostId}")
    @Operation(
        summary = "공동구매 게시글별 정산 목록 조회 (개발중)",
        description = "특정 공동구매 게시글의 정산 목록을 조회합니다."
    )
    public ResponseEntity<List<SettleUpResponse>> getSettleUpsByGroupPost(
            @Parameter(description = "공동구매 게시글 ID", example = "1")
            @PathVariable Long groupPostId,
            @Parameter(description = "활성 정산만 조회 여부 (기본값: false)", example = "true")
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly
    ) {
        log.info("[API 호출] GET /api/settleups/group-post/{} - 활성만: {}", groupPostId, activeOnly);
        List<SettleUpResponse> response = settleUpService.getSettleUpsByGroupPost(groupPostId, activeOnly);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 정산 목록 조회 (페이징)
     *
     * @param principal 인증된 사용자 정보
     * @param activeOnly 활성 정산만 조회 여부 (기본값: false)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK, 내 정산 목록 (페이징)
     */
    @GetMapping("/my")
    @Operation(
        summary = "내 정산 목록 조회 (테스트용)",
        description = "현재 로그인한 사용자가 생성한 정산 목록을 조회합니다. (인증 필요) / 1=미정산(기본값), 2=정산완료, 0=전체 정산을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<SettleUpResponse>> getMySettleUps(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "정산 조회 타입 (1: 미정산(기본값), 2: 정산완료, 0: 전체)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer activeOnly,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[API 호출] GET /api/settleups/my - 사용자 ID: {}, 조회타입: {}, 페이지: {}, 크기: {}",
                 principal.id(), activeOnly, page, size);
        Page<SettleUpResponse> response = settleUpService.getSettleUpsByUser(principal.id(), activeOnly, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 정산 목록 조회 (페이징)
     *
     * @param activeOnly 활성 정산만 조회 여부 (기본값: false)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK, 전체 정산 목록 (페이징)
     */
    @GetMapping
    @Operation(
        summary = "전체 정산 목록 조회",
        description = "모든 정산 목록을 조회합니다. 1=미정산(기본값), 2=정산완료, 0=전체 정산을 조회합니다."
    )
    public ResponseEntity<Page<SettleUpResponse>> getAllSettleUps(
            @Parameter(description = "정산 조회 타입 (1: 미정산(기본값), 2: 정산완료, 0: 전체)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer activeOnly,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[API 호출] GET /api/settleups - 조회타입: {}, 페이지: {}, 크기: {}", activeOnly, page, size);
        Page<SettleUpResponse> response = settleUpService.getAllSettleUps(activeOnly, page, size);
        return ResponseEntity.ok(response);
    }
}

