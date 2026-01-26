package com.sobunsobun.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 댓글 개수 응답 DTO
 * 특정 게시글의 댓글 총 개수 (삭제되지 않은 댓글만 포함)
 */
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class CommentCountResponse {
    /**
     * 게시글 ID
     */
    private Long postId;

    /**
     * 댓글 개수 (활성 댓글만, 부모 댓글과 대댓글 포함)
     */
    private Long commentCount;
}

