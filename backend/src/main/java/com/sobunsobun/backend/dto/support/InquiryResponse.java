package com.sobunsobun.backend.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 1:1 문의 제출 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponse {

    /**
     * 문의 ID
     */
    private Long inquiryId;

    /**
     * 상태 (PENDING, IN_PROGRESS, COMPLETED)
     */
    private String status;

    /**
     * 안내 메시지
     */
    private String message;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;
}

