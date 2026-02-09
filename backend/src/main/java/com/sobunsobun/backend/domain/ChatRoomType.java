package com.sobunsobun.backend.domain;

/**
 * 채팅방 타입
 * PRIVATE: 개인 채팅 (1:1)
 * GROUP: 단체 채팅 (모임)
 */
public enum ChatRoomType {
    PRIVATE("개인 채팅"),
    GROUP("단체 채팅");

    private final String description;

    ChatRoomType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
