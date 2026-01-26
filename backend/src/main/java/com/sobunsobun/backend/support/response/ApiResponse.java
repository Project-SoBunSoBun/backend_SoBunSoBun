package com.sobunsobun.backend.support.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 통합 API 응답 형식
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private int statusCode;
    private String message;
    private T data;
    private String errorCode;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> errorDetails;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static <T> ApiResponse<T> success(HttpStatus status, T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(status.value())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static <T> ApiResponse<T> error(int statusCode, String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static <T> ApiResponse<T> error(int statusCode, String errorCode, String message,
                                           Map<String, Object> errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .errorDetails(errorDetails)
                .timestamp(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String errorCode, String message) {
        return error(status.value(), errorCode, message);
    }
}

