package com.sobunsobun.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 엔티티
 * 게시글에 달리는 댓글과 대댓글을 관리
 *
 * 계층 구조:
 * - parentComment가 null이면 부모 댓글
 * - parentComment가 있으면 대댓글 (1 depth)
 * - 부모 댓글이 삭제되어도 대댓글은 유지됨
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "comment",
    indexes = {
        @Index(name = "idx_comment_post", columnList = "post_id"),
        @Index(name = "idx_comment_user", columnList = "user_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_comment_id"),
        @Index(name = "idx_comment_deleted", columnList = "deleted")
    }
)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 게시글
     * 댓글이 달린 게시글
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_post"))
    private GroupPost post;

    /**
     * 댓글 작성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comment_user"))
    private User user;

    /**
     * 부모 댓글 (대댓글인 경우에만 설정)
     * null이면 부모 댓글, null이 아니면 대댓글
     *
     * 부모 댓글이 삭제되어도 대댓글은 유지됨
     * (부모의 deleted = true이고 자식은 유지)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", foreignKey = @ForeignKey(name = "fk_comment_parent"))
    private Comment parentComment;

    /**
     * 자식 댓글들 (대댓글 목록)
     * 부모 댓글만 이 필드를 사용
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Comment> childComments = new ArrayList<>();

    /**
     * 댓글 내용
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 사용자의 위치 인증 정보 (주소)
     * 선택사항, 예: "서울시 강남구"
     */
    @Column(length = 500)
    private String verifyLocation;

    /**
     * Soft Delete 여부
     * true: 삭제됨, false: 활성
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

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
     * 대댓글 추가 헬퍼 메서드
     */
    public void addChildComment(Comment childComment) {
        childComments.add(childComment);
        childComment.setParentComment(this);
    }

    /**
     * 자신의 정보가 유효한지 확인 (삭제되지 않음)
     */
    public boolean isActive() {
        return !deleted;
    }

    /**
     * 작성자가 일치하는지 확인
     */
    public boolean isAuthor(User user) {
        return this.user.getId().equals(user.getId());
    }
}

