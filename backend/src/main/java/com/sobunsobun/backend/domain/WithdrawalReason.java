package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회원 탈퇴 사유 기록 엔티티
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "withdrawal_reason",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_reason_code", columnList = "reason_code"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class WithdrawalReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 탈퇴한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawal_reason_user"))
    private User user;

    /**
     * 탈퇴 사유 코드
     */
    @Column(name = "reason_code", nullable = false, length = 30)
    private String reasonCode;

    /**
     * 탈퇴 사유 상세
     */
    @Column(name = "reason_detail", length = 100)
    private String reasonDetail;

    /**
     * 탈퇴 시점의 이메일
     */
    @Column(name = "email", length = 190)
    private String email;

    /**
     * 탈퇴 시점의 닉네임
     */
    @Column(name = "nickname", length = 40)
    private String nickname;

    /**
     * 탈퇴 시점의 매너 점수
     */
    @Column(name = "manner_score", precision = 3, scale = 2)
    private BigDecimal mannerScore;

    /**
     * 탈퇴 시점의 주소
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

