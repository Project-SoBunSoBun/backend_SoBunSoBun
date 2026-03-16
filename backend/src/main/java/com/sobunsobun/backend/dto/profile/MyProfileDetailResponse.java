package com.sobunsobun.backend.dto.profile;

import com.sobunsobun.backend.dto.post.PostListResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 내 프로필 상세 조회 응답 DTO
 *
 * tab 파라미터에 따라 posts 필드에 다른 목록이 채워집니다:
 * - "posts"     : 내가 작성한 게시글
 * - "commented" : 내가 댓글을 단 게시글
 * - "saved"     : 내가 저장한 게시글
 */
@Getter
@Builder
public class MyProfileDetailResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Integer activityScore;
    private Integer hostCount;
    private Integer participationCount;
    private List<MannerTagDto> mannerTags;

    /** 현재 조회 탭 (posts / commented / saved) */
    private String tab;

    /** 탭에 해당하는 페이징된 게시글 목록 */
    private PostListResponse posts;

    @Getter
    @Builder
    public static class MannerTagDto {
        private Integer tagId;
        private String label;
        private Integer count;
    }
}
