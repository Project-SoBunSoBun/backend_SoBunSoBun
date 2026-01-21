package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 디바이스 (FCM 토큰) 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_device",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_device", columnNames = {"user_id", "device_id"}),
           @UniqueConstraint(name = "uk_fcm_token", columnNames = {"fcm_token"})
       },
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_is_enabled", columnList = "is_enabled")
       })
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 (외래키)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_device_user"))
    private User user;

    /**
     * 디바이스 고유 ID (UUID)
     */
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /**
     * FCM 토큰
     */
    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    /**
     * 플랫폼 (IOS, ANDROID)
     */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /**
     * 앱 버전
     */
    @Column(name = "app_version", length = 20)
    private String appVersion;

    /**
     * OS 버전
     */
    @Column(name = "os_version", length = 20)
    private String osVersion;

    /**
     * 활성화 여부
     */
    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    /**
     * 마지막 확인 일시
     */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 토큰 갱신
     */
    public void updateToken(String fcmToken, String appVersion, String osVersion) {
        this.fcmToken = fcmToken;
        this.appVersion = appVersion;
        this.osVersion = osVersion;
        this.lastSeenAt = LocalDateTime.now();
    }

    /**
     * 활성화/비활성화
     */
    public void setEnabled(Boolean enabled) {
        this.isEnabled = enabled;
    }
}

