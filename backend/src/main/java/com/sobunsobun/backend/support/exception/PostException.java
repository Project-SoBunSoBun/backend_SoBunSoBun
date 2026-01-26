package com.sobunsobun.backend.support.exception;

/**
 * 게시글 관련 예외
 *
 * ErrorCode enum을 기반으로 생성됩니다.
 */
public class PostException extends BusinessException {

    /**
     * ErrorCode를 사용한 생성
     */
    public PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 커스텀 메시지를 사용한 생성
     */
    public PostException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // =================================================
    // 정적 팩토리 메서드
    // =================================================

    /**
     * 게시글을 찾을 수 없음
     */
    public static PostException notFound() {
        return new PostException(ErrorCode.POST_NOT_FOUND);
    }

    /**
     * 게시글 접근 권한 없음
     */
    public static PostException forbidden() {
        return new PostException(ErrorCode.UNAUTHORIZED_POST_ACCESS);
    }

    /**
     * 이미 삭제된 게시글
     */
    public static PostException alreadyDeleted() {
        return new PostException(ErrorCode.POST_ALREADY_DELETED);
    }

    /**
     * 유효하지 않은 게시글 데이터
     */
    public static PostException badRequest(String message) {
        return new PostException(ErrorCode.INVALID_POST_DATA, message);
    }

    /**
     * 유효하지 않은 게시글 상태
     */
    public static PostException invalidStatus(String message) {
        return new PostException(ErrorCode.INVALID_POST_STATUS, message);
    }
}

