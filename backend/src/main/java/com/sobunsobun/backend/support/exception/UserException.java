package com.sobunsobun.backend.support.exception;

/**
 * 사용자 관련 예외
 *
 * ErrorCode enum을 기반으로 생성됩니다.
 */
public class UserException extends BusinessException {

    /**
     * ErrorCode를 사용한 생성
     */
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 커스텀 메시지를 사용한 생성
     */
    public UserException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // =================================================
    // 정적 팩토리 메서드
    // =================================================

    /**
     * 사용자를 찾을 수 없음
     */
    public static UserException notFound() {
        return new UserException(ErrorCode.USER_NOT_FOUND);
    }

    /**
     * 사용자가 이미 존재함
     */
    public static UserException alreadyExists() {
        return new UserException(ErrorCode.USER_ALREADY_EXISTS);
    }

    /**
     * 유효하지 않은 닉네임
     */
    public static UserException invalidNickname(String message) {
        return new UserException(ErrorCode.INVALID_NICKNAME, message);
    }

    /**
     * 닉네임이 이미 사용 중임
     */
    public static UserException nicknameAlreadyExists() {
        return new UserException(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    /**
     * 탈퇴한 사용자
     */
    public static UserException withdrawn() {
        return new UserException(ErrorCode.USER_WITHDRAWN);
    }

    /**
     * 정지된 사용자
     */
    public static UserException suspended() {
        return new UserException(ErrorCode.USER_SUSPENDED);
    }
}

