package com.sobunsobun.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConnectionTest implements ApplicationRunner {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 1. Redis에 데이터 저장 시도
            redisTemplate.opsForValue().set("windows:test:connection", "성공적으로 연결되었습니다!");

            // 2. Redis에서 데이터 꺼내오기 시도
            String result = (String) redisTemplate.opsForValue().get("windows:test:connection");

            log.info("========================================");
            log.info("🎉 Redis 연결 100% 성공! 가져온 값: {}", result);
            log.info("========================================");
        } catch (Exception e) {
            log.error("❌ Redis 연결 실패: {}", e.getMessage());
        }
    }
}
