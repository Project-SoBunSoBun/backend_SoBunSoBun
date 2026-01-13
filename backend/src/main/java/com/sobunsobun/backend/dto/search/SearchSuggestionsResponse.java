package com.sobunsobun.backend.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 추천 검색어 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "추천 검색어 응답")
public class SearchSuggestionsResponse {

    @Schema(
            description = "요청한 검색 키워드 (없으면 null)",
            example = "치",
            nullable = true
    )
    private String keyword;

    @Schema(
            description = "추천 검색어 목록",
            example = "[\"치킨\", \"휴지\", \"세제\"]"
    )
    private List<String> suggestions;

    @Schema(
            description = "반환된 추천어 개수",
            example = "10"
    )
    private int count;

    /**
     * 추천 검색어 생성 (keyword 없는 경우)
     */
    public static SearchSuggestionsResponse of(List<String> suggestions) {
        return SearchSuggestionsResponse.builder()
                .keyword(null)
                .suggestions(suggestions)
                .count(suggestions.size())
                .build();
    }

    /**
     * 추천 검색어 생성 (keyword 있는 경우)
     */
    public static SearchSuggestionsResponse of(String keyword, List<String> suggestions) {
        return SearchSuggestionsResponse.builder()
                .keyword(keyword)
                .suggestions(suggestions)
                .count(suggestions.size())
                .build();
    }
}

