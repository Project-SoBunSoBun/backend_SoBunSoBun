package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.support.exception.BusinessException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 핸들러
 *
 * 애플리케이션 전체에서 발생하는 예외를 처리하고
 * 통일된 ApiResponse 형식으로 에러 응답을 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * ErrorCode enum 기반 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] Code: {}, Message: {}", e.getCode(), e.getMessage(), e);

        ApiResponse<?> response = ApiResponse.error(
                e.getStatus(),
                e.getCode(),
                e.getErrorMessage()
        );

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    /**
     * ResponseStatusException 처리
     * Spring Framework 표준 예외 처리
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<?>> handleResponseStatusException(ResponseStatusException e) {
        log.error("[ResponseStatusException] Status: {}, Reason: {}", e.getStatusCode(), e.getReason(), e);

        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        ApiResponse<?> response = ApiResponse.error(
                status.value(),
                mapStatusToErrorCode(status),
                e.getReason() != null ? e.getReason() : "Unknown error"
        );

        return ResponseEntity.status(status).body(response);
    }

    /**
     * 유효성 검사 실패 처리 (Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> validationErrors = new HashMap<>();

        // 모든 필드 에러 정보 수집
        e.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("유효성 검사에 실패했습니다");

        log.warn("[MethodArgumentNotValidException] Validation failed: {}", message);

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_REQUEST.getCode(),
                message,
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 데이터 무결성 위반 처리
     * 중복 키, 제약 조건 위반 등
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("[DataIntegrityViolationException] {}", e.getMessage(), e);

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.CONFLICT.value(),
                ErrorCode.DATA_INTEGRITY_VIOLATION.getCode(),
                ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * IllegalArgumentException 처리
     * 비즈니스 로직 위반 (자신의 항목 신고, 중복 신고 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage();
        log.warn("[IllegalArgumentException] {}", message);

        // 자신의 항목 신고 확인
        if (message != null && (message.contains("자신의 게시글") || message.contains("자신의 댓글"))) {
            ApiResponse<?> response = ApiResponse.error(
                    HttpStatus.CONFLICT.value(),
                    ErrorCode.DATA_INTEGRITY_VIOLATION.getCode(),
                    message
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        // 중복 신고 확인
        if (message != null && message.contains("이미")) {
            ApiResponse<?> response = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    ErrorCode.INVALID_REQUEST.getCode(),
                    message
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 리소스 없음 확인
        if (message != null && message.contains("찾을 수 없습니다")) {
            ApiResponse<?> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    message
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 기타 IllegalArgumentException
        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_REQUEST.getCode(),
                message != null ? message : "잘못된 요청입니다"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("[Exception] Unexpected error: {}", e.getMessage(), e);

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * HTTP 상태를 에러 코드로 매핑
     */
    private String mapStatusToErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.INVALID_REQUEST.getCode();
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED.getCode();
            case FORBIDDEN -> ErrorCode.ACCESS_DENIED.getCode();
            case NOT_FOUND -> ErrorCode.RESOURCE_NOT_FOUND.getCode();
            case CONFLICT -> ErrorCode.DATA_INTEGRITY_VIOLATION.getCode();
            case INTERNAL_SERVER_ERROR -> ErrorCode.INTERNAL_SERVER_ERROR.getCode();
            default -> "UNKNOWN_ERROR";
        };
    }
}

