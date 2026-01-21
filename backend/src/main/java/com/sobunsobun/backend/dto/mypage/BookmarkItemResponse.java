package com.sobunsobun.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 북마크 목록 아이템 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkItemResponse {

    /**
     * 게시글 ID
     */
    private Long postId;

    /**
     * 게시글 제목
     */
    private String title;

    /**
     * 썸네일 이미지 URL
     */
    private String thumbnailUrl;

    /**
     * 상태
     */
    private String status;

    /**
     * 총 금액
     */
    private Integer totalAmount;

    /**
     * 1인당 금액
     */
    private Integer unitAmount;

    /**
     * 현재 참여자 수
     */
    private Integer currentParticipants;

    /**
     * 최대 참여자 수
     */
    private Integer maxParticipants;

    /**
     * 방장 닉네임
     */
    private String hostNickname;

    /**
     * 지역
     */
    private String region;

    /**
     * 마감일시
     */
    private LocalDateTime deadline;

    /**
     * 북마크 일시
     */
    private LocalDateTime bookmarkedAt;
}

