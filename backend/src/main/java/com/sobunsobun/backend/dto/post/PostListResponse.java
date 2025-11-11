package com.sobunsobun.backend.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이징된 게시글 목록 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponse {

    /**
     * 게시글 목록
     */
    private List<PostResponse> posts;

    /**
     * 페이징 정보
     */
    private PageInfo pageInfo;

    /**
     * 페이징 정보 (중첩 클래스)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        /**
         * 현재 페이지 번호 (0부터 시작)
         */
        private int currentPage;

        /**
         * 페이지 크기
         */
        private int pageSize;

        /**
         * 전체 게시글 수
         */
        private long totalElements;

        /**
         * 전체 페이지 수
         */
        private int totalPages;

        /**
         * 마지막 페이지 여부
         */
        private boolean isLast;
    }
}

