package com.sobunsobun.backend.exception.chat;

/**
 * 채팅방을 찾을 수 없는 예외
 */
public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(Long roomId) {
        super("채팅방을 찾을 수 없습니다: " + roomId);
    }

    public ChatRoomNotFoundException(String message) {
        super(message);
    }
}
