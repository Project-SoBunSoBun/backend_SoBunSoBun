package com.sobunsobun.backend.domain;

/**
 * 신고 사유 열거형
 */
public enum ReportReason {
    SPAM("스팸 또는 광고"),
    ABUSE("욕설 또는 괴롭힘"),
    INAPPROPRIATE("부적절한 콘텐츠"),
    FRAUD("사기 또는 거짓 정보"),
    HARMFUL("위험한 물품"),
    SCAM("가격 사기"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
