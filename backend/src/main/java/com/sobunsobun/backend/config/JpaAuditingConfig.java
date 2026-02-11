package com.sobunsobun.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화 설정
 *
 * @CreatedDate, @LastModifiedDate 애노테이션이 작동하도록 설정
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
