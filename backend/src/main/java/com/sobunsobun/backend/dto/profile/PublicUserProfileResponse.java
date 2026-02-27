package com.sobunsobun.backend.dto.profile;

import com.sobunsobun.backend.dto.post.PostListResponse;
import lombok.Builder;
import lombok.Getter;

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
    private Double mannerScore;

    /** 해당 유저가 작성한 게시글 목록 (페이징) */
    private PostListResponse posts;
}
