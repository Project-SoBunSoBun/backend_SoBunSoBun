package com.sobunsobun.backend.config;

import com.sobunsobun.backend.infrastructure.redis.RedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // 1. Redis 데이터 처리를 위한 템플릿 설정 (Spring이 알아서 만든 connectionFactory를 주입받음)
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // 직렬화 설정 (데이터가 깨지지 않고 JSON 형태로 예쁘게 저장되도록)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        return redisTemplate;
    }

    // 2. Pub/Sub을 위한 메시지 리스너 컨테이너 설정 (채팅의 핵심)
    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            RedisSubscriber redisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // RedisSubscriber를 Topic 패턴에 등록
        // 패턴: "chat:room:*" - 모든 채팅방의 메시지를 감지
        container.addMessageListener(redisSubscriber, new PatternTopic("chat:room:*"));

        return container;
    }
}
