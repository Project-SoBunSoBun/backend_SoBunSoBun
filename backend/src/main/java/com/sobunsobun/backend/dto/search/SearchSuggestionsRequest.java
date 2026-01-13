package com.sobunsobun.backend.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 검색어 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "추천 검색어 요청")
public class SearchSuggestionsRequest {

    @Schema(
            description = "검색 키워드 (선택사항)",
            example = "치킨",
            nullable = true
    )
    private String keyword;

    @Schema(
            description = "반환할 추천어 개수",
            example = "10",
            defaultValue = "10"
    )
    private Integer limit;

    /**
     * Limit 기본값 설정
     */
    public int getLimitOrDefault() {
        return this.limit != null ? this.limit : 10;
    }

    /**
     * Keyword 유효성 검증
     * - null이거나 빈 문자열이면 false
     * - 공백만 있으면 false
     */
    public boolean isValidKeyword() {
        return this.keyword != null && !this.keyword.trim().isEmpty();
    }
}

