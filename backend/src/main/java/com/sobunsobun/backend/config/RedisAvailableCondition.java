package com.sobunsobun.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis가 사용 가능한지 확인하는 Condition
 *
 * Redis 호스트가 설정되어 있고 실제로 연결 가능한 경우에만 true를 반환합니다.
 */
@Slf4j
public class RedisAvailableCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String redisHost = context.getEnvironment().getProperty("spring.data.redis.host");
        String redisPortStr = context.getEnvironment().getProperty("spring.data.redis.port", "6379");

        // Redis 호스트가 설정되지 않았거나 플레이스홀더인 경우
        if (redisHost == null || redisHost.isEmpty() || redisHost.equals("${redisHost}")) {
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
            log.warn("⚠️ Redis 호스트가 설정되지 않았습니다!");
            log.warn("⚠️ 환경 변수 'redisHost'를 설정하면 Redis를 사용할 수 있습니다.");
            log.warn("⚠️ 채팅 기능을 제외한 다른 API는 정상 작동합니다.");
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
            return false;
        }

        int redisPort;
        try {
            redisPort = Integer.parseInt(redisPortStr);
        } catch (NumberFormatException e) {
            log.warn("⚠️ Redis 포트 파싱 실패: {}", redisPortStr);
            return false;
        }

        // Redis 연결 테스트
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            // 연결 테스트
            factory.getConnection().ping();
            factory.destroy();

            log.info("✅ Redis 사용 가능: {}:{}", redisHost, redisPort);
            return true;
        } catch (Exception e) {
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
            log.warn("⚠️ Redis 서버에 연결할 수 없습니다: {}:{}", redisHost, redisPort);
            log.warn("⚠️ 에러: {}", e.getMessage());
            log.warn("⚠️ 채팅 기능을 사용하려면 Redis 서버를 시작해주세요.");
            log.warn("⚠️ 다른 API는 정상적으로 작동합니다.");
            log.warn("⚠️ ═══════════════════════════════════════════════════════");
            return false;
        }
    }
}
