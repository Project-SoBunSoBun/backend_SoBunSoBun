package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 댓글 신고 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comment_report",
       indexes = {
           @Index(name = "idx_comment_report_user", columnList = "user_id"),
           @Index(name = "idx_comment_report_comment", columnList = "comment_id"),
           @Index(name = "idx_comment_report_status", columnList = "status"),
           @Index(name = "idx_comment_report_created_at", columnList = "created_at")
       })
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_report_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_report_comment"))
    private Comment comment;

    @Column(name = "reason", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
