package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 저장된 게시글 엔티티
 * 사용자가 나중에 보기 위해 게시글을 저장하는 기능
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "saved_post",
       indexes = {
           @Index(name = "idx_saved_post_user", columnList = "user_id"),
           @Index(name = "idx_saved_post_post", columnList = "post_id"),
           @Index(name = "idx_saved_post_created_at", columnList = "created_at")
       })
public class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 저장한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saved_post_user"))
    private User user;

    /**
     * 저장된 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saved_post_post"))
    private GroupPost post;

    /**
     * 저장 일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 기본값 설정
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
