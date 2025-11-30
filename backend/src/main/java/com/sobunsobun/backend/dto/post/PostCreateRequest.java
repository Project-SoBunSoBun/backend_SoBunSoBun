package com.sobunsobun.backend.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공동구매 게시글 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreateRequest {

    /**
     * 게시글 제목 (필수)
     */
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 120, message = "제목은 120자 이내여야 합니다")
    private String title;

    /**
     * 카테고리 ('01', '02' 등)
     */
    @NotBlank(message = "카테고리는 필수입니다")
    @Size(max = 20, message = "카테고리는 최대 20자입니다")
    private String categories;

    /**
     * 게시글 내용
     */
    private String content;

    /**
     * 구매 예정 품목
     */
    private String itemsText;

    /**
     * 전달 사항
     */
    private String notesText;

    /**
     * 만남 장소명
     */
    @Size(max = 120, message = "장소명은 120자 이내여야 합니다")
    private String locationName;

    /**
     * 만남 일시
     */
    private LocalDateTime meetAt;

    /**
     * 모집 마감 일시 (필수)
     */
    @NotNull(message = "마감 일시는 필수입니다")
    private LocalDateTime deadlineAt;

    /**
     * 최소 인원 (기본값: 2)
     */
    private Integer minMembers;

    /**
     * 최대 인원
     */
    private Integer maxMembers;

    /**
     * 현재 참여 인원 (기본값: 1)
     */
    private Integer joinedMembers;
}

