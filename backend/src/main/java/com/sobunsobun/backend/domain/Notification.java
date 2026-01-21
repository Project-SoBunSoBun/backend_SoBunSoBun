package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 알림 내역 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_is_read", columnList = "is_read"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 수신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_user"))
    private User user;

    /**
     * 알림 유형 (CHAT, COMMENT, PARTICIPATION, POST_UPDATE, SETTLEMENT, ANNOUNCEMENT)
     */
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /**
     * 제목
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 내용
     */
    @Column(name = "body", nullable = false, length = 500)
    private String body;

    /**
     * 데이터 페이로드 (JSON 형식)
     * 예: {"type":"CHAT","targetId":"123","deepLink":"sobunsobun://chat/123"}
     */
    @Column(name = "data_payload", columnDefinition = "TEXT")
    private String dataPayload;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 일시
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}

