package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 정산 참여자 내역 엔티티
 *
 * - Settlement 완료 시점에 일괄 생성
 * - 재완료(수정) 시 orphanRemoval로 기존 행 삭제 후 재생성
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "settlement_participant",
        indexes = {
                @Index(name = "idx_sp_settlement", columnList = "settlement_id"),
                @Index(name = "idx_sp_user",       columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sp_settlement_user",
                        columnNames = {"settlement_id", "user_id"})
        }
)
public class SettlementParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sp_settlement"))
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sp_user"))
    private User user;

    /** 이 참여자의 총 부담 금액 */
    @Column(name = "assigned_amount", nullable = false)
    private Long assignedAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementItem> items = new ArrayList<>();

    // =================================================
    // 팩토리 메서드
    // =================================================

    public static SettlementParticipant of(Settlement settlement, User user, long assignedAmount) {
        SettlementParticipant p = new SettlementParticipant();
        p.settlement = settlement;
        p.user = user;
        p.assignedAmount = assignedAmount;
        return p;
    }

    public void addItems(List<SettlementItem> newItems) {
        this.items.addAll(newItems);
    }
}
