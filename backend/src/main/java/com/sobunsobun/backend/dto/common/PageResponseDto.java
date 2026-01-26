package com.sobunsobun.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이지 응답 DTO
 *
 * 페이지 기반 데이터를 반환할 때 사용됩니다.
 *
 * @param <T> 데이터 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseDto<T> {
    /** 페이지 데이터 */
    private List<T> content;

    /** 페이지네이션 정보 */
    private PaginationDto pagination;

    /**
     * 페이지 응답 생성
     */
    public static <T> PageResponseDto<T> of(List<T> content, PaginationDto pagination) {
        return PageResponseDto.<T>builder()
                .content(content)
                .pagination(pagination)
                .build();
    }
}

