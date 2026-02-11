package com.sobunsobun.backend.exception.chat;

/**
 * 채팅방 권한 관련 예외
 */
public class ChatRoomAuthException extends RuntimeException {
    public ChatRoomAuthException(String message) {
        super(message);
    }

    public ChatRoomAuthException(Long roomId, Long userId) {
        super("권한이 없습니다: roomId=" + roomId + ", userId=" + userId);
    }
}
