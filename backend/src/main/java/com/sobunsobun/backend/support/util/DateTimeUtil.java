package com.sobunsobun.backend.support.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 유틸리티
 *
 * 애플리케이션 전체에서 일관된 날짜/시간 처리를 제공합니다.
 */
@Slf4j
public class DateTimeUtil {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(KST_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(KST_ZONE);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(KST_ZONE);

    private DateTimeUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * 현재 시간 (KST) 반환
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(KST_ZONE);
    }

    /**
     * 현재 시간을 ISO-8601 포맷의 문자열로 반환 (KST)
     */
    public static String nowAsString() {
        return now().format(KST_FORMATTER);
    }

    /**
     * LocalDateTime을 ISO-8601 포맷으로 변환
     */
    public static String formatToIso(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(KST_ZONE).format(KST_FORMATTER);
    }

    /**
     * 두 시간 사이의 차이 (초 단위)
     */
    public static long getDifferenceInSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        return java.time.temporal.ChronoUnit.SECONDS.between(startTime, endTime);
    }

    /**
     * 주어진 시간이 과거인지 확인
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime.isBefore(now());
    }

    /**
     * 주어진 시간이 미래인지 확인
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime.isAfter(now());
    }

    /**
     * 시간이 만료되었는지 확인
     */
    public static boolean isExpired(LocalDateTime expiryDateTime) {
        return isPast(expiryDateTime);
    }

    /**
     * 토큰/인증이 유효한 기간 내인지 확인
     */
    public static boolean isValid(LocalDateTime expiryDateTime) {
        return !isExpired(expiryDateTime);
    }
}

