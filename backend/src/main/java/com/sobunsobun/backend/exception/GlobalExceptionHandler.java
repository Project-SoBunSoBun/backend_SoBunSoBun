package com.sobunsobun.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
 * 일관된 형식의 에러 응답을 반환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ExceptionResponse> buildExceptionResponse(Exception ex, HttpStatus status) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Unknown error occurred."
                : ex.getMessage();

        ExceptionResponse body = new ExceptionResponse(status.value(), message);
        return new ResponseEntity<>(body, status);
    }

    /**
     * ResponseStatusException 처리
     * Service 계층에서 던진 비즈니스 예외 처리
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException e) {
        log.error("비즈니스 예외 발생 {}: {}", e.getClass().getSimpleName(), e.getReason());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", getErrorCode(e.getStatusCode().value()));
        errorResponse.put("message", e.getReason());

        return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
    }

    /**
     * 유효성 검사 실패 처리 (Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        // 첫 번째 필드 에러 메시지 사용
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("유효성 검사에 실패했습니다");

        log.error("유효성 검사 실패 {}: {}", e.getClass().getSimpleName(), message);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "validation_failed");
        errorResponse.put("message", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("예상하지 못한 예외 발생 {}: {}", e.getClass().getSimpleName(), e.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "internal_server_error");
        errorResponse.put("message", "서버 오류가 발생했습니다");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * HTTP 상태 코드를 에러 코드로 변환
     */
    private String getErrorCode(int statusCode) {
        return switch (statusCode) {
            case 400 -> "bad_request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not_found";
            case 409 -> "conflict";
            case 500 -> "internal_server_error";
            default -> "error";
        };
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        return buildExceptionResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildExceptionResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ChatAuthException.class)
    public ResponseEntity<ExceptionResponse> handleChatAuthException(ChatAuthException ex) {
        return buildExceptionResponse(ex, HttpStatus.BAD_REQUEST);
    }
}

