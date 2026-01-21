package com.sobunsobun.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이지네이션 응답 DTO
 *
 * @param <T> 컨텐츠 아이템 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    /**
     * 페이지 컨텐츠
     */
    private List<T> content;

    /**
     * 페이지 정보
     */
    private PageInfo page;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageInfo {
        /**
         * 현재 페이지 번호 (0부터 시작)
         */
        private Integer number;

        /**
         * 페이지당 항목 수
         */
        private Integer size;

        /**
         * 전체 항목 수
         */
        private Long totalElements;

        /**
         * 전체 페이지 수
         */
        private Integer totalPages;

        /**
         * 첫 페이지 여부
         */
        private Boolean first;

        /**
         * 마지막 페이지 여부
         */
        private Boolean last;

        /**
         * 다음 페이지 존재 여부
         */
        private Boolean hasNext;

        /**
         * 이전 페이지 존재 여부
         */
        private Boolean hasPrevious;
    }
}

