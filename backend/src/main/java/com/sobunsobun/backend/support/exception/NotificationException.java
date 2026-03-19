package com.sobunsobun.backend.support.exception;

/**
 * 알림 관련 예외
 */
public class NotificationException extends BusinessException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static NotificationException notFound() {
        return new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotificationException accessDenied() {
        return new NotificationException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
    }
}
