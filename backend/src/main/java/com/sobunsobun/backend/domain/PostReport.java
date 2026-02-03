package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 게시글 신고 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "post_report",
       indexes = {
           @Index(name = "idx_post_report_user", columnList = "user_id"),
           @Index(name = "idx_post_report_post", columnList = "post_id"),
           @Index(name = "idx_post_report_status", columnList = "status"),
           @Index(name = "idx_post_report_created_at", columnList = "created_at")
       })
public class PostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 신고한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_report_user"))
    private User user;

    /**
     * 신고된 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_report_post"))
    private GroupPost post;

    /**
     * 신고 사유 (SPAM, ABUSE, INAPPROPRIATE, FRAUD, OTHER)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    /**
     * 신고 상세 내용
     */
    @Column(columnDefinition = "TEXT", length = 1000)
    private String description;

    /**
     * 신고 상태 (PENDING, REVIEWING, RESOLVED, REJECTED, CLOSED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    /**
     * 처리 결과 (승인, 거절 등)
     */
    @Column(length = 255)
    private String resolution;

    /**
     * 처리한 관리자 ID
     */
    @Column(name = "handled_by_admin_id")
    private Long handledByAdminId;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 처리 일시
     */
    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    /**
     * 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }
}
