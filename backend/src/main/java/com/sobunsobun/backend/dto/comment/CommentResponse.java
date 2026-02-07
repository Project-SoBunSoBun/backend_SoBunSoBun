package com.sobunsobun.backend.dto.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sobunsobun.backend.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 응답 DTO
 * 부모 댓글과 대댓글을 트리 구조로 반환
 */
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private String userNickname;
    private String userProfileImageUrl;

    /**
     * 사용자의 주소 (User.address에서 가져옴)
     */
    private String userAddress;

    private String content;

    private Long parentCommentId;
    @Builder.Default
    private List<CommentResponse> childComments = new ArrayList<>();

    /**
     * 삭제 여부
     * true: 삭제됨, false: 활성
     *
     * 프론트에서 표시 규칙: 삭제 상태 우선 표시
     * - deleted = true: "삭제된 댓글입니다" 표시
     * - deleted = false && edited = true: "수정됨" 표시
     * - deleted = false && edited = false: 원본 표시 (수정 표시 없음)
     */
    private Boolean deleted;

    /**
     * 수정 여부
     * true: 수정됨, false: 원본
     *
     * 정책:
     * - deleted = true인 경우, edited는 항상 false (동시에 true가 될 수 없음)
     * - deleted = false && edited = true: "수정됨" 표시 가능
     */
    private Boolean edited;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Comment 엔티티를 DTO로 변환
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPost().getId())
            .userId(comment.getUser().getId())
            .userNickname(comment.getUser().getNickname())
            .userProfileImageUrl(
                comment.getUser().getProfileImageUrl() != null && !comment.getUser().getProfileImageUrl().isEmpty()
                    ? comment.getUser().getProfileImageUrl()
                    : null
            )
            .userAddress(comment.getUser().getAddress())
            .content(comment.getContent())
            .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
            .childComments(comment.getChildComments().stream()
                .filter(Comment::isActive)
                .map(CommentResponse::from)
                .collect(Collectors.toList()))
            .deleted(comment.getDeleted())
            .edited(comment.getEdited())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }

    /**
     * 부모 댓글 기준 대댓글 포함하여 변환
     */
    public static CommentResponse fromWithChildren(Comment comment) {
        if (comment.getParentComment() != null) {
            // 대댓글인 경우
            return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userProfileImageUrl(
                    comment.getUser().getProfileImageUrl() != null && !comment.getUser().getProfileImageUrl().isEmpty()
                        ? comment.getUser().getProfileImageUrl()
                        : null
                )
                .userAddress(comment.getUser().getAddress())
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment().getId())
                .childComments(new ArrayList<>())
                .deleted(comment.getDeleted())
                .edited(comment.getEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
        } else {
            // 부모 댓글인 경우
            return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userProfileImageUrl(
                    comment.getUser().getProfileImageUrl() != null && !comment.getUser().getProfileImageUrl().isEmpty()
                        ? comment.getUser().getProfileImageUrl()
                        : null
                )
                .userAddress(comment.getUser().getAddress())
                .content(comment.getContent())
                .parentCommentId(null)
                .childComments(comment.getChildComments().stream()
                    .filter(Comment::isActive)
                    .map(child -> CommentResponse.builder()
                        .id(child.getId())
                        .postId(child.getPost().getId())
                        .userId(child.getUser().getId())
                        .userNickname(child.getUser().getNickname())
                        .userProfileImageUrl(
                            child.getUser().getProfileImageUrl() != null && !child.getUser().getProfileImageUrl().isEmpty()
                                ? child.getUser().getProfileImageUrl()
                                : null
                        )
                        .userAddress(child.getUser().getAddress())
                        .content(child.getContent())
                        .parentCommentId(comment.getId())
                        .childComments(new ArrayList<>())
                        .deleted(child.getDeleted())
                        .edited(child.getEdited())
                        .createdAt(child.getCreatedAt())
                        .updatedAt(child.getUpdatedAt())
                        .build())
                    .collect(Collectors.toList()))
                .deleted(comment.getDeleted())
                .edited(comment.getEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
        }
    }
}

