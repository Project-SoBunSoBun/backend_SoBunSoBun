package com.sobunsobun.backend.exception.chat;

/**
 * 채팅방 멤버를 찾을 수 없는 예외
 */
public class ChatRoomMemberNotFoundException extends RuntimeException {
    public ChatRoomMemberNotFoundException(Long roomId, Long userId) {
        super("채팅방 멤버를 찾을 수 없습니다: roomId=" + roomId + ", userId=" + userId);
    }

    public ChatRoomMemberNotFoundException(String message) {
        super(message);
    }
}
