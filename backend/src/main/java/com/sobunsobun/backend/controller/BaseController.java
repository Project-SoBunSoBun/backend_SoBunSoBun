package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.support.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 기본 컨트롤러 추상 클래스
 *
 * 모든 컨트롤러가 상속받아 사용할 수 있는 기본 메서드를 제공합니다.
 */
@Slf4j
public abstract class BaseController {

    /**
     * 성공 응답 생성 (200 OK)
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 성공 응답 생성 (메시지 포함)
     */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * 생성 성공 응답 (201 Created)
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, data, "Created successfully"));
    }

    /**
     * 생성 성공 응답 (메시지 포함)
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, data, message));
    }

    /**
     * 수정 성공 응답
     */
    protected <T> ResponseEntity<ApiResponse<T>> updated(T data) {
        return ResponseEntity.ok(ApiResponse.success(data, "Updated successfully"));
    }

    /**
     * 삭제 성공 응답
     */
    protected <T> ResponseEntity<ApiResponse<T>> deleted() {
        return ResponseEntity.ok(ApiResponse.success(null, "Deleted successfully"));
    }

    /**
     * 비어있는 성공 응답 (204 No Content)
     */
    protected <T> ResponseEntity<ApiResponse<T>> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * 에러 응답
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(int statusCode, String errorCode, String message) {
        return ResponseEntity.status(statusCode)
                .body(ApiResponse.error(statusCode, errorCode, message));
    }

    /**
     * 에러 응답 (HttpStatus 포함)
     */
    protected <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String errorCode, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), errorCode, message));
    }
}

