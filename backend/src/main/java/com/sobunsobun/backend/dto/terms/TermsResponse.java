package com.sobunsobun.backend.dto.terms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 약관 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsResponse {

    /**
     * 약관 ID
     */
    private Long id;

    /**
     * 약관 유형 (SERVICE, PRIVACY)
     */
    private String type;

    /**
     * 버전 (예: 1.0.0)
     */
    private String version;

    /**
     * 제목
     */
    private String title;

    /**
     * 내용 (HTML 또는 마크다운)
     */
    private String content;

    /**
     * 필수 동의 여부
     */
    private Boolean isRequired;

    /**
     * 시행일
     */
    private LocalDateTime effectiveDate;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;
}

