package com.sobunsobun.backend.support.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 로깅 유틸리티
 *
 * 애플리케이션 전체에서 일관된 로깅을 제공합니다.
 * 로그 레벨 및 메시지 포맷을 표준화합니다.
 */
@Slf4j
public class LoggingUtil {

    private LoggingUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // =================================================
    // 사용자 작동 관련 로깅
    // =================================================

    /**
     * 사용자 작동 시작 로깅
     *
     * @param action 작동 내용
     * @param userId 사용자 ID (옵션)
     */
    public static void userAction(String action, Long userId) {
        if (userId != null) {
            log.info("[USER_ACTION] {}: userId={}", action, userId);
        } else {
            log.info("[USER_ACTION] {}", action);
        }
    }

    /**
     * 사용자 작동 완료 로깅
     *
     * @param action 작동 내용
     * @param result 결과
     */
    public static void userActionComplete(String action, String result) {
        log.info("[USER_ACTION_COMPLETE] {}: {}", action, result);
    }

    // =================================================
    // 비즈니스 로직 로깅
    // =================================================

    /**
     * 비즈니스 로직 시작 로깅
     *
     * @param businessName 비즈니스 이름
     * @param details 상세 정보
     */
    public static void businessStart(String businessName, String details) {
        log.info("[BUSINESS_START] {}: {}", businessName, details);
    }

    /**
     * 비즈니스 로직 완료 로깅
     *
     * @param businessName 비즈니스 이름
     * @param result 결과
     */
    public static void businessComplete(String businessName, String result) {
        log.info("[BUSINESS_COMPLETE] {}: {}", businessName, result);
    }

    /**
     * 비즈니스 로직 실패 로깅
     *
     * @param businessName 비즈니스 이름
     * @param errorMessage 에러 메시지
     * @param exception 예외 (옵션)
     */
    public static void businessFailed(String businessName, String errorMessage, Throwable exception) {
        if (exception != null) {
            log.error("[BUSINESS_FAILED] {}: {}", businessName, errorMessage, exception);
        } else {
            log.error("[BUSINESS_FAILED] {}: {}", businessName, errorMessage);
        }
    }

    public static void businessFailed(String businessName, String errorMessage) {
        businessFailed(businessName, errorMessage, null);
    }

    // =================================================
    // 데이터베이스 로깅
    // =================================================

    /**
     * 데이터베이스 쿼리 로깅
     *
     * @param operationType 작동 타입 (SELECT, INSERT, UPDATE, DELETE)
     * @param entityName 엔티티 이름
     * @param details 상세 정보
     */
    public static void databaseOperation(String operationType, String entityName, String details) {
        log.debug("[DATABASE] {} on {}: {}", operationType, entityName, details);
    }

    /**
     * 데이터베이스 오류 로깅
     *
     * @param operationType 작동 타입
     * @param entityName 엔티티 이름
     * @param errorMessage 에러 메시지
     * @param exception 예외
     */
    public static void databaseError(String operationType, String entityName,
                                     String errorMessage, Throwable exception) {
        log.error("[DATABASE_ERROR] {} on {}: {} - {}", operationType, entityName,
                errorMessage, exception.getMessage(), exception);
    }

    // =================================================
    // 외부 API 로깅
    // =================================================

    /**
     * 외부 API 호출 로깅
     *
     * @param apiName API 이름
     * @param endpoint 엔드포인트
     * @param details 상세 정보
     */
    public static void apiCall(String apiName, String endpoint, String details) {
        log.info("[API_CALL] {}: {} - {}", apiName, endpoint, details);
    }

    /**
     * 외부 API 응답 로깅
     *
     * @param apiName API 이름
     * @param statusCode HTTP 상태 코드
     * @param details 상세 정보
     */
    public static void apiResponse(String apiName, int statusCode, String details) {
        log.info("[API_RESPONSE] {}: status={} - {}", apiName, statusCode, details);
    }

    /**
     * 외부 API 오류 로깅
     *
     * @param apiName API 이름
     * @param errorMessage 에러 메시지
     * @param exception 예외
     */
    public static void apiError(String apiName, String errorMessage, Throwable exception) {
        log.error("[API_ERROR] {}: {} - {}", apiName, errorMessage,
                exception.getMessage(), exception);
    }

    // =================================================
    // 보안 관련 로깅
    // =================================================

    /**
     * 보안 이벤트 로깅
     *
     * @param eventType 이벤트 타입
     * @param details 상세 정보
     */
    public static void securityEvent(String eventType, String details) {
        log.warn("[SECURITY_EVENT] {}: {}", eventType, details);
    }

    /**
     * 보안 위협 로깅
     *
     * @param threatType 위협 타입
     * @param details 상세 정보
     */
    public static void securityThreat(String threatType, String details) {
        log.error("[SECURITY_THREAT] {}: {}", threatType, details);
    }

    // =================================================
    // 성능 관련 로깅
    // =================================================

    /**
     * 느린 쿼리 로깅
     *
     * @param query 쿼리
     * @param executionTimeMs 실행 시간 (밀리초)
     */
    public static void slowQuery(String query, long executionTimeMs) {
        log.warn("[SLOW_QUERY] Execution time: {}ms - {}", executionTimeMs, query);
    }

    /**
     * 느린 API 호출 로깅
     *
     * @param apiName API 이름
     * @param executionTimeMs 실행 시간 (밀리초)
     */
    public static void slowApiCall(String apiName, long executionTimeMs) {
        log.warn("[SLOW_API] {}: Execution time: {}ms", apiName, executionTimeMs);
    }
}

