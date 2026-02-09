package com.sobunsobun.backend.domain;

/**
 * 채팅방 멤버 상태
 * ACTIVE: 활성 멤버
 * LEFT: 퇴장
 * INVITED: 초대 상태
 */
public enum ChatMemberStatus {
    ACTIVE("활성"),
    LEFT("퇴장"),
    INVITED("초대 대기");

    private final String description;

    ChatMemberStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
