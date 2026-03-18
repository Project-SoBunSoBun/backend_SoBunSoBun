package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 유저 신고 엔티티
 *
 * reporter(신고자)가 targetUser(신고 대상)를 게시글 단위로 신고한 기록을 저장합니다.
 * 동일 게시글에 대해 동일 사용자를 중복 신고할 수 없습니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_report",
       indexes = {
           @Index(name = "idx_user_report_reporter", columnList = "reporter_id"),
           @Index(name = "idx_user_report_target", columnList = "target_user_id"),
           @Index(name = "idx_user_report_status", columnList = "status")
       },
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_user_report_reporter_target_post",
               columnNames = {"reporter_id", "target_user_id", "group_post_id"}
           )
       })
public class UserReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_report_reporter"))
    private User reporter;

    /** 신고 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_report_target"))
    private User targetUser;

    /** 신고 기준 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_post_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_user_report_post"))
    private GroupPost groupPost;

    /** 신고 사유 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    /** 신고 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 처리 상태 (기본값: PENDING) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserReport of(User reporter, User targetUser, GroupPost groupPost, ReportReason reason, String description) {
        UserReport report = new UserReport();
        report.reporter = reporter;
        report.targetUser = targetUser;
        report.groupPost = groupPost;
        report.reason = reason;
        report.description = description;
        report.status = ReportStatus.PENDING;
        return report;
    }
}
