package com.sobunsobun.backend.domain;

/**
 * 채팅 메시지 타입
 * TEXT: 텍스트 메시지
 * IMAGE: 이미지 메시지
 * INVITE_CARD: 초대장 카드
 * SETTLEMENT_CARD: 정산서 카드
 * SYSTEM: 시스템 메시지 (입장, 퇴장 등)
 */
public enum ChatMessageType {
    TEXT("텍스트"),
    IMAGE("이미지"),
    INVITE_CARD("초대장"),
    SETTLEMENT_CARD("정산서"),
    SYSTEM("시스템");

    private final String description;

    ChatMessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
