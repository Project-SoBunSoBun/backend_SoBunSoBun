package com.sobunsobun.backend.controller.post;

import com.sobunsobun.backend.application.post.SavedPostService;
import com.sobunsobun.backend.dto.post.SavedPostDto;
import com.sobunsobun.backend.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 저장된 게시글 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/posts/saved")
@RequiredArgsConstructor
@Tag(name = "Saved Post", description = "저장된 게시글 API")
public class SavedPostController {

    private final SavedPostService savedPostService;

    /**
     * 게시글 저장
     * POST /api/v1/posts/saved
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "게시글 저장", description = "게시글을 저장합니다")
    public ResponseEntity<SavedPostDto.Response> savePost(
            @RequestParam Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        SavedPostDto.Response response = savedPostService.savePost(userId, postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 저장된 게시글 조회
     * GET /api/v1/posts/saved/{savedPostId}
     */
    @GetMapping("/{savedPostId}")
    @Operation(summary = "저장된 게시글 조회", description = "저장된 게시글 상세 정보를 조회합니다")
    public ResponseEntity<SavedPostDto.Response> getSavedPost(
            @PathVariable Long savedPostId) {
        SavedPostDto.Response response = savedPostService.getSavedPost(savedPostId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 저장된 게시글 목록 조회
     * GET /api/v1/posts/saved/my/list
     */
    @GetMapping("/my/list")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 저장된 게시글 목록", description = "현재 사용자가 저장한 게시글 목록을 조회합니다")
    public ResponseEntity<Page<SavedPostDto.ListResponse>> getMySavedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SavedPostDto.ListResponse> response = savedPostService.getMySavedPosts(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 저장 해제
     * DELETE /api/v1/posts/saved
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "게시글 저장 해제", description = "저장된 게시글을 취소합니다")
    public ResponseEntity<Void> unsavePost(
            @RequestParam Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        savedPostService.unsavePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 저장 여부 확인
     * GET /api/v1/posts/saved/check
     */
    @GetMapping("/check")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "게시글 저장 여부 확인", description = "특정 게시글이 저장되었는지 확인합니다")
    public ResponseEntity<Boolean> isSaved(
            @RequestParam Long postId) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean saved = savedPostService.isSaved(userId, postId);
        return ResponseEntity.ok(saved);
    }

    /**
     * 저장된 게시글 개수
     * GET /api/v1/posts/saved/count
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "저장된 게시글 개수", description = "현재 사용자가 저장한 게시글의 개수를 조회합니다")
    public ResponseEntity<Long> countSavedPosts() {
        Long userId = SecurityUtil.getCurrentUserId();
        long count = savedPostService.countSavedPosts(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 게시글의 저장 횟수
     * GET /api/v1/posts/{postId}/saved-count
     */
    @GetMapping("/{postId}/save-count")
    @Operation(summary = "게시글 저장 횟수", description = "특정 게시글이 저장된 횟수를 조회합니다")
    public ResponseEntity<Long> countPostSaves(
            @PathVariable Long postId) {
        long count = savedPostService.countPostSaves(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * 저장된 게시글 통계
     * GET /api/v1/posts/saved/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "저장된 게시글 통계", description = "현재 사용자의 저장된 게시글 통계를 조회합니다")
    public ResponseEntity<SavedPostDto.StatisticsResponse> getSavedPostStatistics() {
        Long userId = SecurityUtil.getCurrentUserId();
        SavedPostDto.StatisticsResponse response = savedPostService.getSavedPostStatistics(userId);
        return ResponseEntity.ok(response);
    }
}
