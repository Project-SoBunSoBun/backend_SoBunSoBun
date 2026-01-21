package com.sobunsobun.backend.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 버그 신고 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugReportResponse {

    /**
     * 버그 신고 ID
     */
    private Long bugReportId;

    /**
     * 상태 (RECEIVED, IN_REVIEW, FIXED, CLOSED)
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

