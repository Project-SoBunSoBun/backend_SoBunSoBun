package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * 소분소분 서비스의 회원 정보를 저장
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "`user`")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이메일 (OAuth에서 받아옴, nullable - 선택 동의 항목)
     */
    @Column(unique = true, length = 190)
    private String email;

    /**
     * 닉네임 (프로필 설정 단계에서 입력, 초기에는 null 가능)
     */
    @Column(unique = true, length = 40)
    private String nickname;

    /**
     * 프로필 이미지 URL (최대 500자)
     */
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * 주소
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * 위치 인증 일시 (마지막 위치 인증 시간)
     */
    @Column(name = "location_verified_at")
    private LocalDateTime locationVerifiedAt;

    /**
     * 매너 점수 (0.00 ~ 5.00)
     */
    @Column(name = "manner_score", precision = 3, scale = 2)
    private BigDecimal mannerScore;

    /**
     * 사용자 권한
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * 사용자 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * OAuth 인증 제공자 목록 (카카오, 애플 등)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuthProvider> authProviders = new ArrayList<>();

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

    /**
     * 탈퇴 일시 (회원 탈퇴 시에만 입력)
     */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /**
     * 재가입 가능 일시 (탈퇴일 + 90일)
     * 이 날짜 이전에는 동일한 OAuth ID로 재가입 불가
     */
    @Column(name = "reactivatable_at")
    private LocalDateTime reactivatableAt;

    /**
     * OAuth 제공자 추가 헬퍼 메서드
     */
    public void addAuthProvider(AuthProvider authProvider) {
        authProviders.add(authProvider);
        authProvider.setUser(this);
    }

    /**
     * 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        if (this.mannerScore == null) {
            this.mannerScore = BigDecimal.ZERO;
        }
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
    }
}
