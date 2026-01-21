package com.sobunsobun.backend.support.exception;

/**
 * 공통 비즈니스 예외 클래스
 */
public class BusinessException extends RuntimeException {
    private final String code;
    private final int status;

    public BusinessException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}

