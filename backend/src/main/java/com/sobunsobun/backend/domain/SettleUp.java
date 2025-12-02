package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 정산 엔티티
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "settle_up",
    indexes = {
        @Index(name = "idx_settle_group_post", columnList = "group_post_id"),
        @Index(name = "idx_settle_settled_by", columnList = "settled_by"),
        @Index(name = "idx_settle_status", columnList = "status")
    }
)
public class SettleUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공동구매 게시글 ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_settle_group_post"))
    private GroupPost groupPost;

    /**
     * 정산 생성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_by", nullable = false, foreignKey = @ForeignKey(name = "fk_settle_user"))
    private User settledBy;

    /**
     * 정산 상태
     * 1: 활성
     * 2: 비활성
     * 3: 삭제됨
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer status = 1;

    /**
     * 정산 제목
     */
    @Column(length = 50)
    private String title;

    /**
     * 만남 장소 이름
     */
    @Column(name = "location_name", length = 120)
    private String locationName;

    /**
     * 만남 시간
     */
    @Column(name = "meet_at")
    private LocalDateTime meetAt;

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
}

