package com.sobunsobun.backend.config;

import com.sobunsobun.backend.infrastructure.redis.RedisSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 *
 * Redis 서버가 없어도 애플리케이션이 시작되도록 설정되어 있습니다.
 * 단, 채팅 기능은 Redis가 필수이므로 Redis 없이는 채팅이 작동하지 않습니다.
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Redis ConnectionFactory 생성
     *
     * RedisAvailableCondition이 true일 때만 생성됩니다.
     */
    @Bean
    @Conditional(RedisAvailableCondition.class)
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        log.info("✅ Redis ConnectionFactory 생성 완료: {}:{}", redisHost, redisPort);

        return factory;
    }

    /**
     * Redis 데이터 처리를 위한 템플릿 설정 (Object 타입)
     *
     * RedisConnectionFactory Bean이 있을 때만 생성됩니다.
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // 직렬화 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        redisTemplate.afterPropertiesSet();

        log.info("✅ RedisTemplate<String, Object> 생성 완료");

        return redisTemplate;
    }

    /**
     * Redis 데이터 처리를 위한 템플릿 설정 (String 타입)
     *
     * ChatRedisService에서 사용하는 String 전용 템플릿입니다.
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // 직렬화 설정 (모두 String으로)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();

        log.info("✅ RedisTemplate<String, String> 생성 완료");

        return redisTemplate;
    }

    /**
     * Pub/Sub을 위한 메시지 리스너 컨테이너 설정
     *
     * 채팅 기능의 핵심 구성 요소입니다.
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            RedisSubscriber redisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // RedisSubscriber를 Topic 패턴에 등록
        container.addMessageListener(redisSubscriber, new PatternTopic("chat:room:*"));

        log.info("✅ Redis Pub/Sub 리스너 등록 완료");

        return container;
    }
}
