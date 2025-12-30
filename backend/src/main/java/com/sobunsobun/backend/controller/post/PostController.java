package com.sobunsobun.backend.controller.post;

import com.sobunsobun.backend.application.post.PostService;
import com.sobunsobun.backend.dto.post.*;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 공동구매 게시글 API 컨트롤러
 *
 * 엔드포인트:
 * - POST   /api/posts                          : 게시글 생성 (인증 필요)
 * - GET    /api/posts                          : 전체 게시글 목록 조회 (공개)
 * - GET    /api/posts/{id}                     : 게시글 단건 조회 (공개)
 * - PUT    /api/posts/{id}                     : 게시글 수정 (인증 필요, 작성자만)
 * - DELETE /api/posts/{id}                     : 게시글 삭제 (인증 필요, 작성자만)
 * - GET    /api/posts/status/{status}          : 상태별 게시글 조회 (공개)
 * - GET    /api/posts/categories/{categories}  : 단일 카테고리 게시글 조회 (공개)
 * - POST   /api/posts/categories/filter        : 여러 카테고리 게시글 조회 (공개, 배열로 전달)
 * - GET    /api/posts/my                       : 내 게시글 목록 조회 (인증 필요)
 *
 * 참고: 카테고리 코드(4자리)는 iOS 클라이언트에서 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "공동구매 게시글 API")
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     *
     * @param principal 인증된 사용자 정보
     * @param request 게시글 생성 요청 데이터
     * @return 201 Created, 생성된 게시글 정보
     */
    @PostMapping
    @Operation(
            summary = "게시글 생성",
            description = "새로운 공동구매 게시글을 생성합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody PostCreateRequest request
    ) {
        log.info("게시글 생성 요청 - 사용자 ID: {}, 제목: {}", principal.id(), request.getTitle());
        PostResponse response = postService.createPost(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 게시글 단건 조회
     *
     * @param postId 게시글 ID
     * @return 200 OK, 게시글 정보
     */
    @GetMapping("/{postId}")
    @Operation(
            summary = "게시글 조회",
            description = "게시글 ID로 단건 조회합니다"
    )
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId
    ) {
        log.info("게시글 조회 요청 - ID: {}", postId);
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 게시글 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK, 페이징된 게시글 목록
     */
    @GetMapping
    @Operation(
            summary = "전체 게시글 목록 조회",
            description = "전체 게시글 목록을 페이징하여 조회합니다 (최신순)"
    )
    public ResponseEntity<PostListResponse> getAllPosts(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("전체 게시글 목록 조회 요청 - 페이지: {}, 크기: {}", page, size);
        PostListResponse response = postService.getAllPosts(page, size);
        log.info("응답 데이터 - 게시글 수: {}, 전체: {}, 페이지: {}/{}",
                 response.getPosts() == null ? 0 : response.getPosts().size(),
                 response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalElements(),
                 response.getPageInfo() == null ? 0 : response.getPageInfo().getCurrentPage(),
                 response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalPages());
        return ResponseEntity.ok(response);
    }

    /**
     * 상태별 게시글 목록 조회
     *
     * @param status 게시글 상태 (OPEN, CLOSED, CANCELLED)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK, 페이징된 게시글 목록
     */
    @GetMapping("/status/{status}")
    @Operation(
            summary = "상태별 게시글 조회",
            description = "게시글 상태별로 목록을 조회합니다 (OPEN, CLOSED, CANCELLED)"
    )
    public ResponseEntity<PostListResponse> getPostsByStatus(
            @Parameter(description = "게시글 상태 (OPEN, CLOSED, CANCELLED)", required = true)
            @PathVariable String status,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("상태별 게시글 목록 조회 요청 - 상태: {}, 페이지: {}, 크기: {}", status, page, size);
        PostListResponse response = postService.getPostsByStatus(status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 카테고리별 게시글 목록 조회 (모집 중인 것만)
     *
     * @param categories 카테고리 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK, 페이징된 게시글 목록
     */
    @GetMapping("/categories/{categories}")
    @Operation(
            summary = "카테고리별 게시글 조회",
            description = "카테고리별 모집 중인 게시글 목록을 조회합니다"
    )
    public ResponseEntity<PostListResponse> getPostsByCategories(
            @Parameter(description = "카테고리 코드 (4자리)", required = true, example = "0001")
            @PathVariable String categories,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("카테고리별 게시글 목록 조회 요청 - 카테고리: {}, 페이지: {}, 크기: {}", categories, page, size);

        // URL 디코딩
        String decoded = URLDecoder.decode(categories, StandardCharsets.UTF_8);

        // 클라이언트(예: iOS)가 배열 표현을 보낼 수 있음: ["0001","0002"] 또는 ["0001", "0002"]
        // 또는 CSV 형태 "0001,0002" 또는 단일 값 "0001"
        try {
            // JSON-like array 처리
            if (decoded.startsWith("[") && decoded.endsWith("]")) {
                String inner = decoded.substring(1, decoded.length() - 1);
                List<String> categoryList = Arrays.stream(inner.split(","))
                        .map(s -> s.trim().replaceAll("^\"|\"$", "")) // 양쪽 큰따옴표 제거
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                // 여러 카테고리 처리 서비스 호출
                PostListResponse response = postService.getPostsByMultipleCategories(categoryList, page, size);
                return ResponseEntity.ok(response);
            }

            // CSV 처리 (쉼표 포함)
            if (decoded.contains(",")) {
                List<String> categoryList = Arrays.stream(decoded.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                PostListResponse response = postService.getPostsByMultipleCategories(categoryList, page, size);
                return ResponseEntity.ok(response);
            }

            // 단일 카테고리
            PostListResponse response = postService.getPostsByCategories(decoded, page, size);
            log.info("단일 카테고리 응답 - 게시글 수: {}, 전체: {}",
                     response.getPosts() == null ? 0 : response.getPosts().size(),
                     response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("카테고리 파싱 오류 - 입력값: {}, 에러: {}", categories, e.toString());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 여러 카테고리로 게시글 목록 조회 (모집 중인 것만)
     * 순수 배열 형식으로 반환
     *
     * @param categoriesList 카테고리 코드 배열
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK, 게시글 배열
     */
    @PostMapping("/categories/filter")
    @Operation(
            summary = "여러 카테고리로 게시글 조회",
            description = "여러 카테고리 코드를 배열로 받아 해당하는 모집 중인 게시글을 배열로 반환합니다"
    )
    public ResponseEntity<List<PostResponse>> getPostsByMultipleCategories(
            @Parameter(description = "카테고리 코드 배열", required = true,
                       example = "[\"0001\", \"0003\", \"0102\", \"0105\", \"0106\"]")
            @RequestBody List<String> categoriesList,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("여러 카테고리 게시글 목록 조회 요청 - 카테고리: {}, 페이지: {}, 크기: {}", categoriesList, page, size);
        PostListResponse response = postService.getPostsByMultipleCategories(categoriesList, page, size);
        // 순수 배열로 반환 (페이징 정보 제외)
        return ResponseEntity.ok(response.getPosts());
    }

    /**
     * 내가 작성한 게시글 목록 조회
     *
     * @param principal 인증된 사용자 정보
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 200 OK, 페이징된 게시글 목록
     */
    @GetMapping("/my")
    @Operation(
            summary = "내 게시글 목록 조회",
            description = "내가 작성한 게시글 목록을 조회합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostListResponse> getMyPosts(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("내 게시글 목록 조회 요청 - 사용자 ID: {}, 페이지: {}, 크기: {}", principal.id(), page, size);
        PostListResponse response = postService.getMyPosts(principal.id(), page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 수정
     *
     * @param postId 게시글 ID
     * @param principal 인증된 사용자 정보
     * @param request 수정 요청 데이터
     * @return 200 OK, 수정된 게시글 정보
     */
    @PutMapping("/{postId}")
    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다 (작성자만 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        log.info("게시글 수정 요청 - 게시글 ID: {}, 사용자 ID: {}", postId, principal.id());
        PostResponse response = postService.updatePost(postId, principal.id(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 게시글 삭제
     *
     * @param postId 게시글 ID
     * @param principal 인증된 사용자 정보
     * @return 204 No Content
     */
    @DeleteMapping("/{postId}")
    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다 (작성자만 가능)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID", required = true)
            @PathVariable Long postId,
            @AuthenticationPrincipal JwtUserPrincipal principal
    ) {
        log.info("게시글 삭제 요청 - 게시글 ID: {}, 사용자 ID: {}", postId, principal.id());
        postService.deletePost(postId, principal.id());
        return ResponseEntity.noContent().build();
    }
}

