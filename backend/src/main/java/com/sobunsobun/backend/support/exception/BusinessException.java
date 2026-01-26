package com.sobunsobun.backend.support.exception;

import lombok.Getter;

/**
 * 공통 비즈니스 예외 클래스
 *
 * 모든 비즈니스 로직 예외의 기본 클래스입니다.
 * ErrorCode enum을 기반으로 일관된 에러 정보를 관리합니다.
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final int status;
    private final String errorMessage;

    /**
     * ErrorCode enum을 사용한 예외 생성
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.status = errorCode.getStatusCode();
        this.errorMessage = errorCode.getMessage();
    }

    /**
     * ErrorCode enum과 커스텀 메시지를 사용한 예외 생성
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.status = errorCode.getStatusCode();
        this.errorMessage = customMessage;
    }

    /**
     * 레거시 호환성을 위한 생성자
     *
     * @deprecated ErrorCode enum을 사용한 생성자를 권장합니다.
     */
    @Deprecated
    public BusinessException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
        this.errorMessage = message;
    }
}

