package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 공동구매 게시글 엔티티
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "group_post",
    indexes = {
        @Index(name = "idx_post_status_deadline", columnList = "status, deadline_at"),
        @Index(name = "idx_post_owner", columnList = "owner_user_id")
    }
)
public class GroupPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 게시글 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_owner"))
    private User owner;

    /**
     * 게시글 제목
     */
    @Column(nullable = false, length = 120)
    private String title;

    /**
     * 카테고리 코드 (4자리, iOS에서 관리)
     * 대분류(2자리) + 소분류(2자리)
     * 예: 식품-과일(0001), 생활용품-디지털/가전(0101)
     */
    @Column(name = "categories", nullable = false, length = 4, columnDefinition = "CHAR(4)")
    private String categories;


    /**
     * 구매 예정 품목
     */
    @Column(name = "items_text", columnDefinition = "TEXT")
    private String itemsText;

    /**
     * 전달 사항
     */
    @Column(name = "notes_text", columnDefinition = "TEXT")
    private String notesText;

    /**
     * 만남 장소명
     */
    @Column(name = "location_name", length = 120)
    private String locationName;

    /**
     * 만남 일시
     */
    @Column(name = "meet_at")
    private LocalDateTime meetAt;

    /**
     * 모집 마감 일시
     */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    /**
     * 최소 인원 (기본 2명)
     */
    @Column(name = "min_members")
    private Integer minMembers;

    /**
     * 최대 인원
     */
    @Column(name = "max_members")
    private Integer maxMembers;


    /**
     * 게시글 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

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
     * 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        if (this.minMembers == null) {
            this.minMembers = 2;
        }
        if (this.status == null) {
            this.status = PostStatus.OPEN;
        }
    }
}
