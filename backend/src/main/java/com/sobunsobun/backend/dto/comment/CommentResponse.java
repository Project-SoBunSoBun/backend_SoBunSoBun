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

    private Boolean deleted;
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
            .userProfileImageUrl(comment.getUser().getProfileImageUrl())
            .userAddress(comment.getUser().getAddress())
            .content(comment.getContent())
            .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
            .childComments(comment.getChildComments().stream()
                .filter(Comment::isActive)
                .map(CommentResponse::from)
                .collect(Collectors.toList()))
            .deleted(comment.getDeleted())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }

    /**
     * 부모 댓글 기준 대댓글 포함하여 변환
     */
    public static CommentResponse fromWithChildren(Comment comment) {
        if (comment.getParentComment() != null) {
            return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userProfileImageUrl(comment.getUser().getProfileImageUrl())
                .userAddress(comment.getUser().getAddress())
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment().getId())
                .childComments(new ArrayList<>())
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
        } else {
            return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                .userProfileImageUrl(comment.getUser().getProfileImageUrl())
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
                        .userProfileImageUrl(child.getUser().getProfileImageUrl())
                        .userAddress(child.getUser().getAddress())
                        .content(child.getContent())
                        .parentCommentId(comment.getId())
                        .childComments(new ArrayList<>())
                        .deleted(child.getDeleted())
                        .createdAt(child.getCreatedAt())
                        .updatedAt(child.getUpdatedAt())
                        .build())
                    .collect(Collectors.toList()))
                .deleted(comment.getDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
        }
    }
}

