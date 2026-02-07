package com.sobunsobun.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 프로필 조회 응답 DTO
 *
 * 다른 사용자의 프로필 정보를 조회할 때 사용
 * 클릭한 이미지/닉네임으로 해당 유저의 프로필을 확인하는 기능에 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 닉네임
     */
    private String nickname;

    /**
     * 프로필 이미지 URL
     */
    private String profileImageUrl;

    /**
     * 매너 점수 (0.00 ~ 5.00)
     */
    private Double mannerScore;

    /**
     * 공동구매 참여 횟수
     */
    private Integer participationCount;

    /**
     * 방장(개설) 횟수
     */
    private Integer hostCount;

    /**
     * 작성한 글 수
     */
    private Integer postCount;

    /**
     * 받은 매너 평가 태그 목록 (상위 5개)
     */
    private List<MannerTagDto> mannerTags;

    /**
     * 작성한 게시글 목록
     */
    private List<PostItemDto> posts;

    /**
     * 사용자 소개 (향후 추가될 필드)
     */
    private String introduction;

    /**
     * 매너 태그 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MannerTagDto {
        /**
         * 태그 ID
         */
        private Integer tagId;

        /**
         * 받은 횟수
         */
        private Integer count;
    }

    /**
     * 게시글 아이템 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostItemDto {
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
         * 지역
         */
        private String region;

        /**
         * 작성일시
         */
        private LocalDateTime createdAt;

        /**
         * 마감일시
         */
        private LocalDateTime deadline;

        /**
         * 조회수
         */
        private Integer viewCount;

        /**
         * 북마크 수
         */
        private Integer bookmarkCount;
    }
}
