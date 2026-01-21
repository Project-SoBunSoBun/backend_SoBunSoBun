package com.sobunsobun.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 참여한 공동구매 목록 아이템 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationItemResponse {

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
     * 상태 (RECRUITING, IN_PROGRESS, COMPLETED, CANCELLED)
     */
    private String status;

    /**
     * 총 금액
     */
    private Integer totalAmount;

    /**
     * 내 부담 금액
     */
    private Integer myAmount;

    /**
     * 현재 참여자 수
     */
    private Integer participantCount;

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
     * 참여 일시
     */
    private LocalDateTime participatedAt;

    /**
     * 완료 일시
     */
    private LocalDateTime completedAt;
}

