package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.support.exception.BusinessException;
import com.sobunsobun.backend.support.exception.ChatException;
import com.sobunsobun.backend.support.exception.ErrorCode;
import com.sobunsobun.backend.support.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.converter.HttpMessageNotReadableException;

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
     * 채팅 예외 처리
     */
    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<?>> handleChatException(ChatException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[ChatException] Code: {}, Message: {}", errorCode.getCode(), e.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                errorCode.getStatusCode(),
                errorCode.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * Hibernate EntityNotFoundException 처리
     * 삭제된 사용자 등 엔티티 참조 실패 시
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("[EntityNotFoundException] 엔티티를 찾을 수 없습니다: {}", e.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                ErrorCode.USER_NOT_FOUND.getStatusCode(),
                ErrorCode.USER_NOT_FOUND.getCode(),
                ErrorCode.USER_NOT_FOUND.getMessage()
        );

        return ResponseEntity.status(ErrorCode.USER_NOT_FOUND.getHttpStatus()).body(response);
    }

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

        boolean isEmailConflict = e.getMessage() != null && e.getMessage().toLowerCase().contains("email");
        ErrorCode errorCode = isEmailConflict ? ErrorCode.USER_EMAIL_DUPLICATE : ErrorCode.DATA_INTEGRITY_VIOLATION;

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.CONFLICT.value(),
                errorCode.getCode(),
                errorCode.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * JSON 파싱 오류 처리
     * 잘못된 JSON 형식의 요청이 들어온 경우
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[HttpMessageNotReadableException] 잘못된 요청 본문: {}", e.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_REQUEST.getCode(),
                "요청 본문의 JSON 형식이 올바르지 않습니다. 올바른 JSON 형식으로 다시 시도해주세요."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 파일 업로드 크기 초과 처리
     * 최대 파일 크기(10MB) 또는 요청 크기(15MB) 초과 시
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[MaxUploadSizeExceededException] 파일 크기 초과: {}", e.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "FILE_SIZE_EXCEEDED",
                "파일 크기가 허용된 최대 크기(10MB)를 초과했습니다. 더 작은 파일을 업로드해주세요."
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * 리소스 없음 처리 (404)
     * 잘못된 URL 경로 요청 시
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[NoResourceFoundException] {}", e.getMessage());

        ApiResponse<?> response = ApiResponse.error(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                "요청한 리소스를 찾을 수 없습니다."
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * IllegalArgumentException 처리
     * 유효성 검증 실패 등 잘못된 인자 전달 시
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage();
        log.warn("[IllegalArgumentException] {}", message);

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

