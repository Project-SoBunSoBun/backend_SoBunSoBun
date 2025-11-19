package com.sobunsobun.backend.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검색어 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchKeywordRequest {

    /**
     * 검색어
     */
    private String keyword;

    /**
     * 카테고리 (선택적)
     */
    private String category;
}

