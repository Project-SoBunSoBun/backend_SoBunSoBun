package com.sobunsobun.backend.support.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * HTTP 관련 유틸리티
 *
 * HTTP 요청/응답 처리를 위한 헬퍼 메서드를 제공합니다.
 */
@Slf4j
public class HttpUtil {

    private HttpUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    // =================================================
    // 헤더 관련
    // =================================================

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     *
     * @param authorizationHeader Authorization 헤더 값
     * @return Bearer 토큰 (없으면 null)
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring("Bearer ".length());
    }

    /**
     * Authorization 헤더 생성
     *
     * @param token 토큰
     * @return Authorization 헤더 값
     */
    public static String createBearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * Content-Type JSON 헤더 생성
     *
     * @return HTTP 헤더
     */
    public static HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        return headers;
    }

    /**
     * CORS 헤더 추가
     *
     * @param headers HTTP 헤더
     * @param origin 허용할 원본
     */
    public static void addCorsHeaders(HttpHeaders headers, String origin) {
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    // =================================================
    // 상태 코드 관련
    // =================================================

    /**
     * HTTP 상태 코드가 성공인지 확인
     *
     * @param statusCode 상태 코드
     * @return 성공 여부
     */
    public static boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * HTTP 상태 코드가 리다이렉트인지 확인
     *
     * @param statusCode 상태 코드
     * @return 리다이렉트 여부
     */
    public static boolean isRedirectStatus(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }

    /**
     * HTTP 상태 코드가 클라이언트 에러인지 확인
     *
     * @param statusCode 상태 코드
     * @return 클라이언트 에러 여부
     */
    public static boolean isClientErrorStatus(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * HTTP 상태 코드가 서버 에러인지 확인
     *
     * @param statusCode 상태 코드
     * @return 서버 에러 여부
     */
    public static boolean isServerErrorStatus(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }

    // =================================================
    // 재시도 관련
    // =================================================

    /**
     * HTTP 상태 코드가 재시도 가능한지 확인
     *
     * @param statusCode 상태 코드
     * @return 재시도 가능 여부
     */
    public static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    /**
     * HTTP 상태에서 재시도 대기 시간 계산 (밀리초)
     *
     * @param statusCode 상태 코드
     * @param retryCount 재시도 횟수
     * @return 대기 시간 (밀리초)
     */
    public static long calculateRetryDelay(int statusCode, int retryCount) {
        if (statusCode == 429) {
            // Rate limiting: 기본 대기
            return 1000L * (long) Math.pow(2, retryCount);
        }
        // 서버 에러: exponential backoff
        return 500L * (long) Math.pow(2, retryCount);
    }

    // =================================================
    // Content-Type 관련
    // =================================================

    /**
     * Content-Type이 JSON인지 확인
     *
     * @param contentType Content-Type 값
     * @return JSON 여부
     */
    public static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * Content-Type이 이미지인지 확인
     *
     * @param contentType Content-Type 값
     * @return 이미지 여부
     */
    public static boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Content-Type이 폼 데이터인지 확인
     *
     * @param contentType Content-Type 값
     * @return 폼 데이터 여부
     */
    public static boolean isFormDataContentType(String contentType) {
        return contentType != null &&
               (contentType.contains("application/x-www-form-urlencoded") ||
                contentType.contains("multipart/form-data"));
    }
}

