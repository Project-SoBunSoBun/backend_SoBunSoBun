package com.sobunsobun.backend.support.exception;

/**
 * 채팅 관련 비즈니스 예외
 *
 * 사용:
 * - throw new ChatException(CHAT_ROOM_NOT_FOUND);
 * - throw new ChatException(CHAT_ROOM_ACCESS_DENIED);
 */
public class ChatException extends RuntimeException {

    private final ErrorCode errorCode;

    public ChatException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ChatException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ChatException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
