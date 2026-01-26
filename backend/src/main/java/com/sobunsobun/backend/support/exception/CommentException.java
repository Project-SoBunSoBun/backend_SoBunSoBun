package com.sobunsobun.backend.support.exception;

/**
 * 댓글 관련 예외
 *
 * ErrorCode enum을 기반으로 생성됩니다.
 */
public class CommentException extends BusinessException {

    /**
     * ErrorCode를 사용한 생성
     */
    public CommentException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 커스텀 메시지를 사용한 생성
     */
    public CommentException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // =================================================
    // 정적 팩토리 메서드
    // =================================================

    /**
     * 댓글을 찾을 수 없음
     */
    public static CommentException notFound() {
        return new CommentException(ErrorCode.COMMENT_NOT_FOUND);
    }

    /**
     * 댓글 접근 권한 없음 (본인만 수정/삭제 가능)
     */
    public static CommentException forbidden() {
        return new CommentException(ErrorCode.UNAUTHORIZED_COMMENT_ACCESS);
    }

    /**
     * 이미 삭제된 댓글
     */
    public static CommentException alreadyDeleted() {
        return new CommentException(ErrorCode.COMMENT_ALREADY_DELETED);
    }

    /**
     * 유효하지 않은 댓글 데이터
     */
    public static CommentException badRequest(String message) {
        return new CommentException(ErrorCode.INVALID_COMMENT_DATA, message);
    }
}

