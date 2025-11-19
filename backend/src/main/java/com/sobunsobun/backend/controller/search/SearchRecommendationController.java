package com.sobunsobun.backend.controller.search;

import com.sobunsobun.backend.application.search.SearchRecommendationService;
import com.sobunsobun.backend.dto.search.SearchKeywordRequest;
import com.sobunsobun.backend.dto.search.SearchRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 검색 추천 API 컨트롤러
 *
 * 협업 필터링 기반 검색어 추천 및 검색 기록 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "검색 추천 API")
public class SearchRecommendationController {

    private final SearchRecommendationService searchRecommendationService;

    /**
     * 검색 기록 저장
     *
     * @param authentication 인증 정보
     * @param request 검색 요청
     * @return 성공 응답
     */
    @PostMapping("/history")
    @Operation(summary = "검색 기록 저장", description = "사용자의 검색어를 기록에 저장합니다")
    public ResponseEntity<Void> saveSearchHistory(
            Authentication authentication,
            @RequestBody SearchKeywordRequest request
    ) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("[API 호출] POST /api/search/history - 사용자 ID: {}, 검색어: {}",
                userId, request.getKeyword());

        searchRecommendationService.saveSearchHistory(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 검색어 추천 (협업 필터링)
     *
     * @param authentication 인증 정보
     * @param limit 추천 개수 (기본 10개)
     * @return 추천 검색어 목록
     */
    @GetMapping("/recommendations")
    @Operation(summary = "검색어 추천",
               description = "협업 필터링을 활용하여 사용자에게 맞춤 검색어를 추천합니다")
    public ResponseEntity<SearchRecommendationResponse> getRecommendations(
            Authentication authentication,
            @Parameter(description = "추천 개수")
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("[API 호출] GET /api/search/recommendations - 사용자 ID: {}, 제한: {}",
                userId, limit);

        SearchRecommendationResponse response = searchRecommendationService
                .getRecommendations(userId, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * 인기 검색어 조회
     *
     * @param limit 조회 개수 (기본 10개)
     * @return 인기 검색어 목록
     */
    @GetMapping("/popular")
    @Operation(summary = "인기 검색어", description = "최근 7일간의 인기 검색어를 조회합니다")
    public ResponseEntity<List<String>> getPopularKeywords(
            @Parameter(description = "조회 개수")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("[API 호출] GET /api/search/popular - 제한: {}", limit);

        List<String> popularKeywords = searchRecommendationService.getPopularKeywords(limit);
        return ResponseEntity.ok(popularKeywords);
    }

    /**
     * 최근 검색 기록 조회
     *
     * @param authentication 인증 정보
     * @param limit 조회 개수 (기본 10개)
     * @return 최근 검색어 목록
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 검색 기록", description = "사용자의 최근 검색 기록을 조회합니다")
    public ResponseEntity<List<String>> getRecentSearches(
            Authentication authentication,
            @Parameter(description = "조회 개수")
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("[API 호출] GET /api/search/recent - 사용자 ID: {}, 제한: {}", userId, limit);

        List<String> recentSearches = searchRecommendationService.getRecentSearches(userId, limit);
        return ResponseEntity.ok(recentSearches);
    }

    /**
     * 검색 기록 삭제
     *
     * @param authentication 인증 정보
     * @return 성공 응답
     */
    @DeleteMapping("/history")
    @Operation(summary = "검색 기록 삭제", description = "사용자의 모든 검색 기록을 삭제합니다")
    public ResponseEntity<Void> clearSearchHistory(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("[API 호출] DELETE /api/search/history - 사용자 ID: {}", userId);

        searchRecommendationService.clearSearchHistory(userId);
        return ResponseEntity.ok().build();
    }
}

