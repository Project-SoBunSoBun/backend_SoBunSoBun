package com.sobunsobun.backend.config;

import com.fasterxml.jackson.core.JsonGenerator;
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
     * ObjectMapper 커스터마이징
     *
     * 설정:
     * - LocalDateTime을 ISO 8601 형식으로 직렬화 (+09:00 포함)
     * - 타임존: Asia/Seoul
     * - Timestamp 사용 안 함 (문자열 형식 사용)
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .timeZone("Asia/Seoul")
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeWithOffsetSerializer());
    }
}

