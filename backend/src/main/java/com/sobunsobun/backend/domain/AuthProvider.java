package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * OAuth 인증 제공자 정보 엔티티
 * 사용자의 외부 인증 정보를 저장 (카카오, 애플 등)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "auth_provider",
    uniqueConstraints = @UniqueConstraint(name = "u_provider_user", columnNames = {"provider", "provider_user_id"})
)
public class AuthProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연결된 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_auth_provider_user"))
    private User user;

    /**
     * OAuth 제공자 (KAKAO, APPLE 등)
     */
    @Column(nullable = false, length = 20)
    private String provider;

    /**
     * OAuth 제공자의 사용자 고유 ID (이전 oauth_id)
     */
    @Column(name = "provider_user_id", nullable = false, length = 190)
    private String providerUserId;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

