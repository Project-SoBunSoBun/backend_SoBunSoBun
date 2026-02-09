package com.sobunsobun.backend.dto.support;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 버그 신고 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BugReportRequest {

    /**
     * 버그 유형 코드 (필수)
     */
    @NotBlank(message = "버그 유형은 필수 선택입니다.")
    private String typeCode;

    /**
     * 버그 설명 (필수, 1~1000자)
     */
    @NotBlank(message = "버그 설명은 필수입니다.")
    @Size(min = 1, max = 1000, message = "버그 설명은 1~1000자여야 합니다.")
    private String content;

    /**
     * 답변 받을 이메일 (필수)
     */
    @NotBlank(message = "답변 받을 이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String replyEmail;

    /**
     * 첨부 스크린샷 이미지 (선택, 최대 5개)
     */
    private List<MultipartFile> screenshots;

    /**
     * 디바이스 정보 (선택)
     */
    private DeviceInfo deviceInfo;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeviceInfo {
        /**
         * 기기 모델 (예: iPhone 15 Pro)
         */
        private String model;

        /**
         * OS 버전 (예: iOS 17.2)
         */
        private String osVersion;

        /**
         * 앱 버전 (예: 1.2.0)
         */
        private String appVersion;
    }
}

