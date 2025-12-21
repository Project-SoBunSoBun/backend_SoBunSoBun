package com.sobunsobun.backend.dto.post;

import com.sobunsobun.backend.domain.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공동구매 게시글 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    /**
     * 게시글 ID
     */
    private Long id;

    /**
     * 작성자 정보
     */
    private OwnerInfo owner;

    /**
     * 게시글 제목
     */
    private String title;

    /**
     * 카테고리 코드 (4자리, iOS에서 관리)
     */
    private String categoryCode;

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
    private String locationName;

    /**
     * 만남 일시
     */
    private LocalDateTime meetAt;

    /**
     * 모집 마감 일시
     */
    private LocalDateTime deadlineAt;

    /**
     * 최소 인원
     */
    private Integer minMembers;

    /**
     * 최대 인원
     */
    private Integer maxMembers;

    /**
     * 현재 참여 인원
     */
    private Integer joinedMembers;

    /**
     * 게시글 상태
     */
    private PostStatus status;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    private LocalDateTime updatedAt;

    /**
     * 작성자 정보 (중첩 클래스)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnerInfo {
        private Long id;
        private String nickname;
        private String profileImageUrl;
    }
}

