package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 정산 엔티티 (1:1 with GroupPost)
 *
 * - 게시글 생성 시 PENDING 상태로 자동 생성
 * - iOS 클라이언트가 최종 계산 결과 전송 시 COMPLETED로 전환
 * - 게시글 삭제 시 DB CASCADE로 함께 삭제
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "settlement",
        indexes = {
                @Index(name = "idx_settlement_status", columnList = "status"),
                @Index(name = "idx_settlement_post",   columnList = "group_post_id")
        }
)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 공동구매 게시글 (1:1)
     * DB FK: ON DELETE CASCADE (group_post 삭제 시 settlement도 자동 삭제)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "group_post_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_settlement_group_post")
    )
    private GroupPost groupPost;

    /**
     * 총 정산 금액 (PENDING 상태에서는 null)
     */
    @Column(name = "total_amount")
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING;

    /**
     * 참여자별 내역 (COMPLETED 시 채워짐)
     * orphanRemoval: 재제출 시 기존 참여자 데이터 자동 삭제
     */
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementParticipant> participants = new ArrayList<>();

    // =================================================
    // 팩토리 메서드
    // =================================================

    /**
     * 게시글 생성 시 호출 — PENDING 상태로 초기화
     */
    public static Settlement createFor(GroupPost groupPost) {
        Settlement s = new Settlement();
        s.groupPost = groupPost;
        s.status = SettlementStatus.PENDING;
        return s;
    }

    // =================================================
    // 도메인 메서드
    // =================================================

    /**
     * iOS 최종 데이터로 정산 완료 처리
     * 기존 participants를 orphanRemoval로 모두 제거 후 새로 추가
     */
    public void complete(long totalAmount, List<SettlementParticipant> newParticipants) {
        this.totalAmount = totalAmount;
        this.participants.clear();
        this.participants.addAll(newParticipants);
        this.status = SettlementStatus.COMPLETED;
    }

    public boolean isCompleted() {
        return this.status == SettlementStatus.COMPLETED;
    }
}
