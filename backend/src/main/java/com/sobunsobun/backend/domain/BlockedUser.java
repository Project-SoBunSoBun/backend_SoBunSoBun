package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 엔티티
 *
 * blocker(차단한 사람)와 blocked(차단 당한 사람)의 관계를 저장합니다.
 * (blocker_id, blocked_id) 조합에 유니크 제약을 걸어 DB 레벨에서 중복 차단을 방지합니다.
 */
@Entity
@Table(
    name = "blocked_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 차단을 건 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    // 차단 당한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    public static BlockedUser of(User blocker, User blocked) {
        BlockedUser entity = new BlockedUser();
        entity.blocker = blocker;
        entity.blocked = blocked;
        return entity;
    }
}
