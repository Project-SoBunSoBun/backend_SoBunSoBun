package com.sobunsobun.backend.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로필 수정 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String message;
    private LocalDateTime updatedAt;
}

