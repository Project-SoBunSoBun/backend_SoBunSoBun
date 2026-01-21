package com.sobunsobun.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 요청 DTO
 */
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCommentRequest {
    /**
     * 댓글 내용
     * 필수, 1자 이상 1000자 이하
     */
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다.")
    private String content;
}

