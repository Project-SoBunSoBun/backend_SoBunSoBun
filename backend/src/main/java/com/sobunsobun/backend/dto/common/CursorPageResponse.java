package com.sobunsobun.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cursor 기반 페이지네이션 응답 DTO
 * 무한 스크롤에 적합
 *
 * @param <T> 컨텐츠 아이템 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorPageResponse<T> {

    /**
     * 페이지 컨텐츠
     */
    private List<T> content;

    /**
     * Cursor 정보
     */
    private CursorInfo cursor;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CursorInfo {
        /**
         * 다음 페이지 cursor (Base64 인코딩된 값)
         */
        private String next;

        /**
         * 다음 페이지 존재 여부
         */
        private Boolean hasNext;

        /**
         * 전체 항목 수 (선택, 성능 고려 시 제외 가능)
         */
        private Long totalElements;
    }
}

