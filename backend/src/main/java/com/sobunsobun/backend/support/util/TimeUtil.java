package com.sobunsobun.backend.support.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {

    private TimeUtil() {}

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // Instant → KST ISO 문자열 ("2025-10-11T15:00:00+09:00")
    public static String normalizeToKstIso(Instant instant) {
        if (instant == null) {
            return null;
        }

        return instant
                .atZone(KST)             // UTC → KST
                .toOffsetDateTime()
                .truncatedTo(ChronoUnit.SECONDS) // 밀리초 제거
                .toString();             // "2025-10-11T15:00:00+09:00"
    }

    // 필요하면 String용 기존 함수도 유지 가능
    // (다른 곳에서 쓰고 있으면)
    public static String normalizeToKstIso(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Instant instant = Instant.parse(raw);
            return normalizeToKstIso(instant);
        } catch (DateTimeParseException ignore) {}
        return raw;
    }
}