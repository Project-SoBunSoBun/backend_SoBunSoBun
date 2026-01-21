package com.sobunsobun.backend.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 계정 정보 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfoResponse {

    /**
     * 사용자 고유 ID
     */
    private Long userId;

    /**
     * 이메일
     */
    private String email;

    /**
     * 이메일 인증 여부
     */
    private Boolean emailVerified;

    /**
     * 연동된 인증 제공자 목록
     */
    private List<AuthProviderInfo> authProviders;

    /**
     * 계정 생성 일시
     */
    private LocalDateTime createdAt;

    /**
     * 마지막 로그인 일시
     */
    private LocalDateTime lastLoginAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthProviderInfo {
        /**
         * 인증 제공자 (KAKAO, APPLE)
         */
        private String provider;

        /**
         * 연동 일시
         */
        private LocalDateTime linkedAt;
    }
}

