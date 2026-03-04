package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 정산 품목 상세 엔티티
 *
 * - SettlementParticipant 1개당 N개의 품목
 * - iOS가 계산한 참여자별 구매 품목 내역
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "settlement_item",
        indexes = {
                @Index(name = "idx_si_participant", columnList = "participant_id")
        }
)
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_si_participant"))
    private SettlementParticipant participant;

    /** 품목명 */
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    /** 수량 또는 중량 (소수 지원: 0.5kg 등) */
    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    /** 단위: "개", "g", "kg", "ml" 등 */
    @Column(length = 20)
    private String unit;

    /** 이 품목의 금액 */
    @Column(nullable = false)
    private Long amount;

    // =================================================
    // 팩토리 메서드
    // =================================================

    public static SettlementItem of(SettlementParticipant participant,
                                    String itemName,
                                    BigDecimal quantity,
                                    String unit,
                                    long amount) {
        SettlementItem item = new SettlementItem();
        item.participant = participant;
        item.itemName = itemName;
        item.quantity = quantity;
        item.unit = unit;
        item.amount = amount;
        return item;
    }
}
