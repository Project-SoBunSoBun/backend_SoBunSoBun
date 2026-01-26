package com.sobunsobun.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 생성 요청 DTO
 * 위치 정보(주소)는 선택사항
 */
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCommentRequest {
    /**
     * 댓글 내용
     * 필수, 1자 이상 1000자 이하
     */
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(min = 1, max = 1000, message = "댓글은 1자 이상 1000자 이하여야 합니다.")
    private String content;

    /**
     * 부모 댓글 ID (대댓글인 경우만 설정)
     * - null 또는 0이면 부모 댓글
     * - 양수이면 대댓글 (부모 댓글 ID)
     */
    private Long parentCommentId;

    /**
     * 사용자의 위치 인증 정보 (주소)
     * 선택사항, 예: "서울시 강남구", "부산시 해운대구" 등
     * 최대 500자
     */
    @Size(max = 500, message = "위치는 500자 이하여야 합니다.")
    private String verifyLocation;

    /**
     * Post-processing: parentCommentId가 0이면 null로 변환
     */
    public void normalizeParentCommentId() {
        if (parentCommentId != null && parentCommentId <= 0) {
            this.parentCommentId = null;
        }
    }
}


