package com.sobunsobun.backend.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 검색어 추천 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRecommendationResponse {

    /**
     * 추천 검색어 목록
     */
    private List<RecommendedKeyword> recommendations;

    /**
     * 인기 검색어 목록
     */
    private List<String> popularKeywords;

    /**
     * 추천 타입 (collaborative: 협업 필터링, popular: 인기 검색어)
     */
    private String recommendationType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedKeyword {
        /**
         * 검색어
         */
        private String keyword;

        /**
         * 추천 점수 (0.0 ~ 1.0)
         */
        private Double score;

        /**
         * 추천 이유
         */
        private String reason;
    }
}

