package com.sobunsobun.backend.dto.profile;

import com.sobunsobun.backend.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 내 프로필 댓글 목록용 경량 댓글 응답 DTO
 */
@Getter
@Builder
public class MyCommentResponse {

    private Long id;
    private Long postId;
    private String postTitle;
    private String content;

    /** null이면 일반 댓글, 값이 있으면 대댓글 */
    private Long parentCommentId;

    private LocalDateTime createdAt;

    public static MyCommentResponse from(Comment comment) {
        return MyCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .content(comment.getContent())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
