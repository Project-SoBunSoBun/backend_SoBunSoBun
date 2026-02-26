package com.sobunsobun.backend.support.exception;

/**
 * 인증/인가 관련 예외
 *
 * ErrorCode enum을 기반으로 생성됩니다.
 */
public class AuthenticationException extends BusinessException {

    /**
     * ErrorCode를 사용한 생성
     */
    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * ErrorCode와 커스텀 메시지를 사용한 생성
     */
    public AuthenticationException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }

    // =================================================
    // 정적 팩토리 메서드
    // =================================================

    /**
     * 인증이 필요함
     */
    public static AuthenticationException unauthorized() {
        return new AuthenticationException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 유효하지 않은 토큰
     */
    public static AuthenticationException invalidToken() {
        return new AuthenticationException(ErrorCode.INVALID_TOKEN);
    }

    /**
     * 토큰 만료
     */
    public static AuthenticationException tokenExpired() {
        return new AuthenticationException(ErrorCode.TOKEN_EXPIRED);
    }

    /**
     * 접근 권한 없음
     */
    public static AuthenticationException accessDenied() {
        return new AuthenticationException(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 카카오 로그인 실패
     */
    public static AuthenticationException kakaoLoginFailed(String message) {
        return new AuthenticationException(ErrorCode.KAKAO_LOGIN_FAILED, message);
    }

    /**
     * 이메일 동의 필요
     */
    public static AuthenticationException emailConsentRequired() {
        return new AuthenticationException(ErrorCode.EMAIL_CONSENT_REQUIRED);
    }

    /**
     * Apple 계정 연결 해제 실패 (Apple 서버 오류)
     */
    public static AuthenticationException appleRevokeFailed(String message) {
        return new AuthenticationException(ErrorCode.APPLE_REVOKE_FAILED, message);
    }

    /**
     * Apple refresh_token 없음 (id_token 직접 전달 방식으로 로그인한 경우)
     */
    public static AuthenticationException appleRefreshTokenNotFound() {
        return new AuthenticationException(ErrorCode.APPLE_REFRESH_TOKEN_NOT_FOUND);
    }
}

