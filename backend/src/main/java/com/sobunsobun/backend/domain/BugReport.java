package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 버그 신고 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bug_report",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 신고자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bug_report_user"))
    private User user;

    /**
     * 버그 유형 코드
     */
    @Column(name = "type_code", nullable = false, length = 30)
    private String typeCode;

    /**
     * 버그 설명
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 답변 받을 이메일
     */
    @Column(name = "reply_email", nullable = false, length = 100)
    private String replyEmail;

    /**
     * 첨부 이미지 URL (JSON 배열 형식)
     */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    /**
     * 디바이스 정보 (JSON 형식)
     */
    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    /**
     * 상태 (RECEIVED, IN_REVIEW, FIXED, CLOSED)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "RECEIVED";

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

