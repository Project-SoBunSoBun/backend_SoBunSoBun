package com.sobunsobun.backend.controller.search;

import com.sobunsobun.backend.application.search.SearchService;
import com.sobunsobun.backend.dto.post.PostListResponse;
import com.sobunsobun.backend.dto.post.PostSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 검색 API 컨트롤러
 *
 * 엔드포인트:
 * - GET /api/search                    : 게시글 검색 (공개)
 * - GET /api/search/status/{status}    : 상태별 게시글 검색 (공개)
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "검색", description = "게시글 검색 API")
public class SearchController {

    private final SearchService searchService;

    /**
     * 게시글 검색 (OPEN 상태만)
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     *
     * @param keyword 검색 키워드
     * @param sortBy 정렬 기준 (latest: 최신순, deadline: 마감임박순)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK, 검색 결과 목록
     */
    @GetMapping
    @Operation(
            summary = "게시글 검색 (OPEN 상태만)",
            description = "제목, 카테고리, 품목, 장소명 중 하나라도 검색어가 포함된 OPEN 상태의 게시글을 조회합니다"
    )
    public ResponseEntity<PostListResponse> searchPosts(
            @Parameter(description = "검색 키워드", required = true, example = "치킨")
            @RequestParam String keyword,
            @Parameter(description = "정렬 기준 (latest: 최신순, deadline: 마감임박순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sortBy,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[API 호출] GET /api/search - 키워드: '{}', 정렬: {}, 페이지: {}, 크기: {}",
                keyword, sortBy, page, size);

        PostSearchRequest request = PostSearchRequest.builder()
                .keyword(keyword)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        PostListResponse response = searchService.searchPosts(request);

        log.info("[API 응답] 검색 결과 - 게시글 수: {}, 전체: {}, 페이지: {}/{}",
                response.getPosts() == null ? 0 : response.getPosts().size(),
                response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalElements(),
                response.getPageInfo() == null ? 0 : response.getPageInfo().getCurrentPage() + 1,
                response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * 상태별 게시글 검색
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색되며,
     * 특정 상태의 게시글만 필터링됩니다
     *
     * @param status 게시글 상태 (OPEN, CLOSED, CANCELLED)
     * @param keyword 검색 키워드
     * @param sortBy 정렬 기준 (latest: 최신순, deadline: 마감임박순)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 200 OK, 검색 결과 목록
     */
    @GetMapping("/status/{status}")
    @Operation(
            summary = "상태별 게시글 검색",
            description = "특정 상태의 게시글 중에서 검색어가 포함된 게시글을 조회합니다"
    )
    public ResponseEntity<PostListResponse> searchPostsByStatus(
            @Parameter(description = "게시글 상태 (OPEN, CLOSED, CANCELLED)", required = true)
            @PathVariable String status,
            @Parameter(description = "검색 키워드", required = true, example = "치킨")
            @RequestParam String keyword,
            @Parameter(description = "정렬 기준 (latest: 최신순, deadline: 마감임박순)", example = "latest")
            @RequestParam(defaultValue = "latest") String sortBy,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[API 호출] GET /api/search/status/{} - 키워드: '{}', 정렬: {}, 페이지: {}, 크기: {}",
                status, keyword, sortBy, page, size);

        PostListResponse response = searchService.searchPostsByStatus(keyword, status, sortBy, page, size);

        log.info("[API 응답] 검색 결과 - 게시글 수: {}, 전체: {}, 페이지: {}/{}",
                response.getPosts() == null ? 0 : response.getPosts().size(),
                response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalElements(),
                response.getPageInfo() == null ? 0 : response.getPageInfo().getCurrentPage() + 1,
                response.getPageInfo() == null ? 0 : response.getPageInfo().getTotalPages());

        return ResponseEntity.ok(response);
    }
}

