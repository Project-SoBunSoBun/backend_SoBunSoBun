package com.sobunsobun.backend.application.search;

import com.sobunsobun.backend.dto.search.SearchKeywordRequest;
import com.sobunsobun.backend.dto.search.SearchRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 검색 추천 서비스
 *
 * 검색 기록을 로그 파일에만 저장하고 추천 기능은 추후 구현 예정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchRecommendationService {


    /**
     * 검색 기록 저장 (로그 파일에만 기록)
     *
     * @param userId 사용자 ID
     * @param request 검색 요청
     */
    public void saveSearchHistory(Long userId, SearchKeywordRequest request) {
        log.info("[사용자 작동] 검색 기록 저장 - 사용자 ID: {}, 검색어: {}", userId, request.getKeyword());

        String normalizedKeyword = request.getKeyword().trim().toLowerCase();

        // 로그 파일에만 검색 기록 저장
        log.info("[검색 기록] 사용자 ID: {}, 검색어: {}, 카테고리: {}",
                userId, normalizedKeyword, request.getCategory());
    }

    /**
     * 검색어 추천 (현재는 빈 목록 반환, 추후 로그 파일 분석 기반 구현 예정)
     *
     * @param userId 사용자 ID
     * @param limit 추천 개수
     * @return 검색 추천 응답
     */
    public SearchRecommendationResponse getRecommendations(Long userId, int limit) {
        log.info("[사용자 작동] 검색어 추천 조회 - 사용자 ID: {}, 제한: {}", userId, limit);

        // 현재는 빈 추천 목록 반환 (추후 로그 파일 분석 기반으로 구현 예정)
        return SearchRecommendationResponse.builder()
                .recommendations(Collections.emptyList())
                .popularKeywords(Collections.emptyList())
                .recommendationType("none")
                .build();
    }

    /**
     * 인기 검색어 조회 (현재는 빈 목록 반환)
     *
     * @param limit 조회 개수
     * @return 인기 검색어 목록
     */
    public List<String> getPopularKeywords(int limit) {
        log.info("[사용자 작동] 인기 검색어 조회 - 제한: {}", limit);
        // 추후 로그 파일 분석 기반으로 구현 예정
        return Collections.emptyList();
    }

    /**
     * 사용자의 최근 검색 기록 조회 (현재는 빈 목록 반환)
     *
     * @param userId 사용자 ID
     * @param limit 조회 개수
     * @return 최근 검색어 목록
     */
    public List<String> getRecentSearches(Long userId, int limit) {
        log.info("[사용자 작동] 최근 검색 기록 조회 - 사용자 ID: {}, 제한: {}", userId, limit);
        // 추후 로그 파일 분석 기반으로 구현 예정
        return Collections.emptyList();
    }

    /**
     * 사용자의 검색 기록 삭제 (로그 파일은 삭제하지 않음)
     *
     * @param userId 사용자 ID
     */
    public void clearSearchHistory(Long userId) {
        log.info("[사용자 작동] 검색 기록 삭제 요청 - 사용자 ID: {} (로그 파일은 유지됨)", userId);
        // 검색 기록은 로그 파일에만 저장되므로 별도 삭제 불필요
    }
}

