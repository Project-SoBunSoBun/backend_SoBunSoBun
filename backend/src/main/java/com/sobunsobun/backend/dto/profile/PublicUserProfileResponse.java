package com.sobunsobun.backend.dto.profile;

import com.sobunsobun.backend.dto.post.PostListResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 타 유저 프로필 조회 응답 DTO
 *
 * 해당 유저의 기본 정보와 작성 게시글 목록을 반환합니다.
 */
@Getter
@Builder
public class PublicUserProfileResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Integer activityScore;
    private Integer hostCount;
    private Integer participationCount;
    private List<MannerTagDto> mannerTags;

    /** 해당 유저가 작성한 게시글 목록 (페이징) */
    private PostListResponse posts;

    @Getter
    @Builder
    public static class MannerTagDto {
        private Integer tagId;
        private String label;
        private Integer count;
    }
}
