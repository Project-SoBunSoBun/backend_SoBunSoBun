package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 유저별 태그 카운트 통계 엔티티 (user_tag_stats 테이블)
 *
 * 프로필 조회 성능을 위해 manner_review를 매번 집계하지 않고,
 * 리뷰 저장 시점에 카운트를 +1하여 이 테이블을 최신 상태로 유지합니다.
 *
 * UPSERT 전략:
 * - INSERT ... ON DUPLICATE KEY UPDATE count = count + 1
 * - UNIQUE(receiver_id, tag_code) 제약 조건이 UPSERT의 기준입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "user_tag_stats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_tag_stats_receiver_tag",
            columnNames = {"receiver_id", "tag_code"}
        )
    },
    indexes = {
        @Index(name = "idx_user_tag_stats_receiver_count", columnList = "receiver_id, count DESC")
    }
)
public class UserTagStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 태그를 받은 사용자 ID
     * (User 엔티티 직접 참조 대신 ID만 저장 - UPSERT 쿼리에서 FK join 불필요)
     */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /**
     * 받은 태그 코드
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tag_code", nullable = false, length = 20)
    private MannerTag tagCode;

    /**
     * 누적 받은 횟수
     */
    @Column(nullable = false)
    @Builder.Default
    private int count = 0;
}