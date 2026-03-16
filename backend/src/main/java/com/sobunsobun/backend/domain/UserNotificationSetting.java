package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 알림 설정 엔티티
 * User와 1:1 관계, User의 PK를 공유
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_notification_setting")
public class UserNotificationSetting {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    /**
     * 전체 푸시 알림 ON/OFF
     * false인 경우 다른 설정과 무관하게 모든 푸시 발송 안 함
     */
    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    /**
     * 채팅 알림 ON/OFF
     */
    @Column(name = "chat_enabled", nullable = false)
    @Builder.Default
    private Boolean chatEnabled = true;

    /**
     * 마케팅 알림 ON/OFF
     */
    @Column(name = "marketing_enabled", nullable = false)
    @Builder.Default
    private Boolean marketingEnabled = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
