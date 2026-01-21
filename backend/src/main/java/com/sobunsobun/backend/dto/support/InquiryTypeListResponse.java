package com.sobunsobun.backend.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 문의 유형 목록 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryTypeListResponse {

    private List<TypeItem> types;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypeItem {
        /**
         * 유형 코드
         */
        private String code;

        /**
         * 유형 라벨
         */
        private String label;
    }
}

