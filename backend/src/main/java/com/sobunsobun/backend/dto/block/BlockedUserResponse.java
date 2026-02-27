package com.sobunsobun.backend.dto.block;

import com.sobunsobun.backend.domain.BlockedUser;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 차단된 사용자 응답 DTO
 *
 * 차단 목록 조회 시 클라이언트에 노출할 최소한의 정보만 담습니다.
 * 엔티티를 직접 노출하지 않고 정적 팩토리 from()으로 변환합니다.
 */
@Getter
@Builder
public class BlockedUserResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime blockedAt;

    public static BlockedUserResponse from(BlockedUser blockedUser) {
        return BlockedUserResponse.builder()
            .userId(blockedUser.getBlocked().getId())
            .nickname(blockedUser.getBlocked().getNickname())
            .profileImageUrl(blockedUser.getBlocked().getProfileImageUrl())
            .blockedAt(blockedUser.getCreatedAt())
            .build();
    }
}
