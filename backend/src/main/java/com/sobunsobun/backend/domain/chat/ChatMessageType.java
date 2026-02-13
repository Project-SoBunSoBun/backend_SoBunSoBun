package com.sobunsobun.backend.domain.chat;

public enum ChatMessageType {
    TEXT,              // 텍스트 메시지
    IMAGE,             // 이미지 메시지
    INVITE_CARD,       // 초대장 카드
    SETTLEMENT_CARD,   // 정산서 카드
    SYSTEM,            // 시스템 메시지
    ENTER,             // 채팅방 입장
    LEAVE              // 채팅방 퇴장
}
