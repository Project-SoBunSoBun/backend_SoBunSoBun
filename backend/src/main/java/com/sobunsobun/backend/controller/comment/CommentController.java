package com.sobunsobun.backend.controller.comment;

import com.sobunsobun.backend.application.comment.CommentService;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.comment.CommentResponse;
import com.sobunsobun.backend.dto.comment.CreateCommentRequest;
import com.sobunsobun.backend.dto.comment.UpdateCommentRequest;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 댓글 API Controller
 *
 * 제공 기능:
 * - 댓글 생성 (대댓글 포함)
 * - 댓글 목록 조회 (트리 구조)
 * - 댓글 수정
 * - 댓글 삭제 (Soft Delete)
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Comment API", description = "게시글 댓글 관리 API")
public class CommentController {
    private final CommentService commentService;
    private final UserRepository userRepository;

    /**
     * 1. 댓글 작성
     * POST /api/posts/{postId}/comments
     *
     * 로그인한 사용자만 댓글 작성 가능
     * 대댓글 작성도 이 엔드포인트로 처리 (parentCommentId 필드 사용)
     *
     * @param postId 게시글 ID
     * @param request 댓글 생성 요청
     * @param authentication 인증 정보
     * @return 생성된 댓글 정보
     */
    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 생성", description = "게시글에 댓글을 작성합니다. 대댓글도 이 엔드포인트로 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "댓글 생성 성공",
            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (내용 없음, 대댓글의 대댓글 등)"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "404", description = "게시글 또는 부모 댓글을 찾을 수 없음")
    })
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable @Parameter(description = "게시글 ID") Long postId,
        @Valid @RequestBody CreateCommentRequest request,
        Authentication authentication) {

        log.info("댓글 생성 요청 - postId: {}", postId);

        // parentCommentId가 0이면 null로 변환
        request.normalizeParentCommentId();

        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다"));
        CommentResponse response = commentService.createComment(postId, request, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. 댓글 목록 조회
     * GET /api/posts/{postId}/comments
     *
     * 삭제되지 않은 댓글만 조회
     * 부모 댓글 → 대댓글 트리 구조로 반환
     * 최신순 정렬 (부모 댓글 기준)
     *
     * @param postId 게시글 ID
     * @return 댓글 목록
     */
    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "게시글의 모든 댓글을 트리 구조로 조회합니다 (부모 댓글 → 대댓글). 최신순 정렬.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<List<CommentResponse>> getComments(
        @PathVariable @Parameter(description = "게시글 ID") Long postId) {

        log.info("댓글 목록 조회 - postId: {}", postId);

        List<CommentResponse> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 3. 댓글 수정
     * PATCH /api/comments/{commentId}
     *
     * 작성자 본인만 수정 가능
     *
     * @param commentId 댓글 ID
     * @param request 수정 요청
     * @param authentication 인증 정보
     * @return 수정된 댓글 정보
     */
    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다. 작성자 본인만 수정 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = CommentResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "410", description = "이미 삭제된 댓글")
    })
    public ResponseEntity<CommentResponse> updateComment(
        @PathVariable @Parameter(description = "댓글 ID") Long commentId,
        @Valid @RequestBody UpdateCommentRequest request,
        Authentication authentication) {

        log.info("댓글 수정 요청 - commentId: {}", commentId);

        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다"));
        CommentResponse response = commentService.updateComment(commentId, request, user);

        return ResponseEntity.ok(response);
    }

    /**
     * 4. 댓글 삭제 (Soft Delete)
     * DELETE /api/comments/{commentId}
     *
     * 작성자 본인만 삭제 가능
     * deleted = true로 처리 (데이터 보존)
     *
     * 부모 댓글 삭제 시에도 대댓글은 유지됨
     *
     * @param commentId 댓글 ID
     * @param authentication 인증 정보
     * @return 상태 코드 200
     */
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다 (Soft Delete). 작성자 본인만 삭제 가능합니다. 부모 댓글 삭제 시에도 대댓글은 유지됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않음"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (작성자가 아님)"),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @ApiResponse(responseCode = "410", description = "이미 삭제된 댓글")
    })
    public ResponseEntity<Void> deleteComment(
        @PathVariable @Parameter(description = "댓글 ID") Long commentId,
        Authentication authentication) {

        log.info("댓글 삭제 요청 - commentId: {}", commentId);

        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다"));
        commentService.deleteComment(commentId, user);

        return ResponseEntity.ok().build();
    }
}

