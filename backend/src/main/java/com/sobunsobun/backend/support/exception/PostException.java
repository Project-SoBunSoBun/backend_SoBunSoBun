package com.sobunsobun.backend.support.exception;

/**
 * 게시글 관련 예외
 */
public class PostException extends BusinessException {
    public PostException(String code, String message, int status) {
        super(code, message, status);
    }

    // 게시글을 찾을 수 없음
    public static PostException notFound() {
        return new PostException("POST_NOT_FOUND", "게시글을 찾을 수 없습니다.", 404);
    }

    // 잘못된 요청
    public static PostException badRequest(String message) {
        return new PostException("POST_BAD_REQUEST", message, 400);
    }
}

