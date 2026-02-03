package com.sobunsobun.backend.domain;

/**
 * 신고 상태 열거형
 */
public enum ReportStatus {
    PENDING("대기 중"),
    REVIEWING("검토 중"),
    RESOLVED("해결됨"),
    REJECTED("거절됨"),
    CLOSED("종료됨");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
