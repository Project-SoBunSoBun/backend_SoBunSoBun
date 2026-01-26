package com.sobunsobun.backend.support.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 전역 에러 코드 정의
 *
 * 에러 코드를 enum으로 중앙 관리하여
 * 일관된 에러 응답 형식을 보장합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 인증 관련 (1000~1099)
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("AUTH_003", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    KAKAO_LOGIN_FAILED("AUTH_005", "카카오 로그인 실패", HttpStatus.UNAUTHORIZED),
    EMAIL_CONSENT_REQUIRED("AUTH_006", "이메일 동의가 필요합니다.", HttpStatus.UNAUTHORIZED),

    // 사용자 관련 (2000~2099)
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "이미 가입된 사용자입니다.", HttpStatus.CONFLICT),
    INVALID_NICKNAME("USER_003", "유효하지 않은 닉네임입니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_ALREADY_EXISTS("USER_004", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    USER_WITHDRAWN("USER_005", "탈퇴한 사용자입니다.", HttpStatus.FORBIDDEN),
    USER_SUSPENDED("USER_006", "정지된 사용자입니다.", HttpStatus.FORBIDDEN),

    // 게시글 관련 (3000~3099)
    POST_NOT_FOUND("POST_001", "게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POST_ALREADY_DELETED("POST_002", "이미 삭제된 게시글입니다.", HttpStatus.GONE),
    INVALID_POST_STATUS("POST_003", "유효하지 않은 게시글 상태입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_POST_ACCESS("POST_004", "게시글 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_POST_DATA("POST_005", "유효하지 않은 게시글 데이터입니다.", HttpStatus.BAD_REQUEST),

    // 댓글 관련 (4000~4099)
    COMMENT_NOT_FOUND("COMMENT_001", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_ALREADY_DELETED("COMMENT_002", "이미 삭제된 댓글입니다.", HttpStatus.GONE),
    UNAUTHORIZED_COMMENT_ACCESS("COMMENT_003", "댓글 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_COMMENT_DATA("COMMENT_004", "유효하지 않은 댓글 데이터입니다.", HttpStatus.BAD_REQUEST),

    // 파일 관련 (5000~5099)
    FILE_UPLOAD_FAILED("FILE_001", "파일 업로드 실패", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("FILE_002", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_FILE_TYPE("FILE_003", "유효하지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("FILE_004", "파일 크기가 초과했습니다.", HttpStatus.BAD_REQUEST),

    // 검색 관련 (6000~6099)
    SEARCH_FAILED("SEARCH_001", "검색 실패", HttpStatus.BAD_REQUEST),
    INVALID_SEARCH_QUERY("SEARCH_002", "유효하지 않은 검색 쿼리입니다.", HttpStatus.BAD_REQUEST),

    // 정산 관련 (7000~7099)
    SETTLEUP_NOT_FOUND("SETTLEUP_001", "정산 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_SETTLEUP_STATUS("SETTLEUP_002", "유효하지 않은 정산 상태입니다.", HttpStatus.BAD_REQUEST),

    // 비즈니스 로직 (8000~8099)
    BUSINESS_RULE_VIOLATION("BIZ_001", "비즈니스 규칙 위반", HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION("BIZ_002", "데이터 무결성 위반", HttpStatus.CONFLICT),

    // 서버 에러 (9000~9099)
    INTERNAL_SERVER_ERROR("SERVER_001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("SERVER_002", "서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_ERROR("SERVER_003", "데이터베이스 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 기타 (9500~9999)
    INVALID_REQUEST("INVALID_001", "유효하지 않은 요청입니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("NOT_FOUND_001", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_001", "허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    /**
     * HTTP 상태 코드를 정수로 반환
     */
    public int getStatusCode() {
        return httpStatus.value();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

