package com.sobunsobun.backend.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공동구매 게시글 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSearchRequest {

    /**
     * 검색 키워드
     * title, categories, itemsText, locationName 중 하나라도 포함되면 검색됨
     */
    private String keyword;

    /**
     * 정렬 기준 (latest: 최신순, deadline: 마감임박순, 기본값: latest)
     */
    @Builder.Default
    private String sortBy = "latest";

    /**
     * 페이지 번호 (0부터 시작, 기본값: 0)
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * 페이지 크기 (기본값: 20)
     */
    @Builder.Default
    private Integer size = 20;
}

