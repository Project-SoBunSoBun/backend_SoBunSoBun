package com.sobunsobun.backend.support.exception;

/**
 * 댓글 관련 예외
 */
public class CommentException extends BusinessException {
    public CommentException(String code, String message, int status) {
        super(code, message, status);
    }

    // 댓글을 찾을 수 없음
    public static CommentException notFound() {
        return new CommentException("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.", 404);
    }

    // 권한 없음 (본인만 수정/삭제 가능)
    public static CommentException forbidden() {
        return new CommentException("COMMENT_FORBIDDEN", "댓글을 수정/삭제할 권한이 없습니다.", 403);
    }

    // 이미 삭제된 댓글
    public static CommentException alreadyDeleted() {
        return new CommentException("COMMENT_ALREADY_DELETED", "이미 삭제된 댓글입니다.", 410);
    }

    // 잘못된 요청
    public static CommentException badRequest(String message) {
        return new CommentException("COMMENT_BAD_REQUEST", message, 400);
    }
}

