package com.sobunsobun.backend.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson JSON 직렬화/역직렬화 설정
 *
 * DateTime을 ISO 8601 형식으로 반환 (예: 2025-10-11T15:00:00+09:00)
 * KST 타임존 오프셋 포함
 */
@Configuration
public class JacksonConfig {

    /**
     * ISO 8601 형식의 DateTimeFormatter (타임존 오프셋 포함)
     * 예: 2025-10-11T15:00:00+09:00
     */
    private static final DateTimeFormatter ISO_FORMATTER_WITH_OFFSET =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /**
     * KST 타임존
     */
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * LocalDateTime을 KST 타임존 오프셋과 함께 직렬화하는 커스텀 Serializer
     */
    public static class LocalDateTimeWithOffsetSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                // LocalDateTime을 ZonedDateTime으로 변환 (KST)
                ZonedDateTime zonedDateTime = value.atZone(KST_ZONE);
                // ISO 8601 형식으로 출력 (타임존 오프셋 포함)
                gen.writeString(zonedDateTime.format(ISO_FORMATTER_WITH_OFFSET));
            }
        }
    }

    /**
     * 다양한 형식의 날짜 문자열을 LocalDateTime으로 역직렬화하는 커스텀 Deserializer
     * 지원 형식:
     * - ISO 8601 with UTC: "2025-11-21T15:54:58.064Z"
     * - ISO 8601 with offset: "2025-11-21T15:54:58+09:00"
     * - Standard format: "2025-11-21 15:54:58"
     */
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter STANDARD_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getText();

            if (dateString == null || dateString.trim().isEmpty()) {
                return null;
            }

            try {
                // ISO 8601 형식 (UTC 'Z' 포함) 처리: "2025-11-21T15:54:58.064Z"
                if (dateString.contains("T") && dateString.endsWith("Z")) {
                    return ZonedDateTime.parse(dateString).withZoneSameInstant(KST_ZONE).toLocalDateTime();
                }
                // ISO 8601 형식 (타임존 오프셋 포함) 처리: "2025-11-21T15:54:58+09:00"
                else if (dateString.contains("T") && (dateString.contains("+") || dateString.lastIndexOf("-") > 10)) {
                    return ZonedDateTime.parse(dateString).withZoneSameInstant(KST_ZONE).toLocalDateTime();
                }
                // ISO 8601 기본 형식 처리: "2025-11-21T15:54:58"
                else if (dateString.contains("T")) {
                    return LocalDateTime.parse(dateString);
                }
                // 표준 형식 처리: "2025-11-21 15:54:58"
                else {
                    return LocalDateTime.parse(dateString, STANDARD_FORMATTER);
                }
            } catch (DateTimeParseException e) {
                throw new IOException("날짜 형식을 파싱할 수 없습니다: " + dateString, e);
            }
        }
    }

    /**
     * ObjectMapper 커스터마이징
     *
     * 설정:
     * - LocalDateTime을 ISO 8601 형식으로 직렬화 (+09:00 포함)
     * - 다양한 ISO 8601 형식을 LocalDateTime으로 역직렬화
     * - 타임존: Asia/Seoul
     * - Timestamp 사용 안 함 (문자열 형식 사용)
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .timeZone("Asia/Seoul")
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeWithOffsetSerializer())
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer());
    }
}

