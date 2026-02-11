package com.sobunsobun.backend.exception.chat;

/**
 * 잘못된 채팅방 작업 예외
 */
public class InvalidChatRoomException extends RuntimeException {
    public InvalidChatRoomException(String message) {
        super(message);
    }
}
