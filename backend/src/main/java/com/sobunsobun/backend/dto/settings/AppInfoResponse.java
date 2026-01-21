package com.sobunsobun.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 앱 버전 정보 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppInfoResponse {

    /**
     * 플랫폼 (IOS, ANDROID)
     */
    private String platform;

    /**
     * 현재 설치된 앱 버전 (클라이언트가 제공한 경우)
     */
    private String currentVersion;

    /**
     * 최신 앱 버전
     */
    private String latestVersion;

    /**
     * 최소 지원 버전
     */
    private String minimumVersion;

    /**
     * 업데이트 필수 여부
     */
    private Boolean updateRequired;

    /**
     * 앱스토어 URL
     */
    private String updateUrl;

    /**
     * 릴리즈 노트
     */
    private String releaseNotes;
}

