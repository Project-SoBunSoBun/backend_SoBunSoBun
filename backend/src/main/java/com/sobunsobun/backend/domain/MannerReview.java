package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 매너 평가 로그 엔티티 (manner_review 테이블)
 *
 * 누가(sender), 누구에게(receiver), 어떤 거래(groupPost)에서,
 * 어떤 태그(tagCode)를 남겼는지 기록합니다.
 *
 * 중복 방지:
 * - UNIQUE(sender_id, receiver_id, group_post_id, tag_code)
 * - 동일 거래에서 동일 태그를 중복 평가할 수 없습니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "manner_review",
    indexes = {
        @Index(name = "idx_manner_review_receiver", columnList = "receiver_id"),
        @Index(name = "idx_manner_review_sender", columnList = "sender_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_manner_review_sender_receiver_post_tag",
            columnNames = {"sender_id", "receiver_id", "group_post_id", "tag_code"}
        )
    }
)
public class MannerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 평가를 남긴 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_manner_review_sender"))
    private User sender;

    /**
     * 평가를 받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_manner_review_receiver"))
    private User receiver;

    /**
     * 평가가 이루어진 공동구매 게시글
     * 동일 거래에 대한 중복 평가 방지 기준점
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_post_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_manner_review_post"))
    private GroupPost groupPost;

    /**
     * 선택한 매너 태그 코드 (e.g., "TAG001")
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_code", nullable = false, length = 20)
    private MannerTag tagCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
