package com.sobunsobun.backend.domain;

/**
 * 초대장 상태
 * PENDING: 대기 중
 * ACCEPTED: 수락됨
 * DECLINED: 거절됨
 * EXPIRED: 만료됨
 */
public enum ChatInviteStatus {
    PENDING("대기"),
    ACCEPTED("수락"),
    DECLINED("거절"),
    EXPIRED("만료");

    private final String description;

    ChatInviteStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
