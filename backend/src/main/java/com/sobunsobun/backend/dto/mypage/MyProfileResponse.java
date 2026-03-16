package com.sobunsobun.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 마이페이지 프로필 응답 DTO
 *
 * 사용자의 프로필 정보와 활동 통계, 매너 태그를 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyProfileResponse {

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
     * 활동 점수 (방장 횟수 × 3 + 참여 횟수 × 2 + 받은 매너 평가 태그 수 × 1 - 신고당한 횟수 × 5, 음수 가능)
     */
    private Integer activityScore;

    /**
     * 공동구매 참여 횟수
     */
    private Integer participationCount;

    /**
     * 방장(개설) 횟수
     */
    private Integer hostCount;

    /**
     * 받은 매너 평가 태그 목록 (상위 5개)
     */
    private List<MannerTagDto> mannerTags;

    /**
     * 매너 태그 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MannerTagDto {
        /**
         * 태그 ID (MannerTag.id 값)
         */
        private Integer tagId;

        /**
         * 태그 라벨 (e.g., "시간 약속을 잘 지켜요")
         */
        private String label;

        /**
         * 받은 횟수
         */
        private Integer count;
    }
}

